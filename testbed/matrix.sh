#!/usr/bin/env bash
#
# Initially created by Claude Code (Opus 5.5) in Oct 2026.
#
# Runs the Grengine unit tests against a matrix of Java and Groovy versions,
# using the jars built by Gradle (no changes to the Gradle build needed).
#
# Usage: testbed/matrix.sh [-b|--build] [-j|--java 17,25] [-g|--groovy 4,6.0.0]
#
#   -b, --build    run './gradlew clean jar testClasses' first
#   -j, --java     comma-separated Java majors (default: 17,21,25)
#   -g, --groovy   comma-separated Groovy versions or version prefixes
#                  (default: 2.5.23,2.5.23-indy,3.0.25,3.0.25-indy,
#                  4.0.33,5.1.3,6.0.0)
#
# The -indy cells use the indy Groovy jar and compile with
# -Dgroovy.target.indy=true (Groovy 4+ is always indy).
#
# JDKs are taken from SDKMAN (~/.sdkman/candidates/java, newest per major,
# non-fx preferred); override per major with e.g. MATRIX_JAVA_HOME_17=/path.
# Groovy 6+ is tested once with groovy-grape-ivy and once with
# groovy-grape-maven as the Grape engine (dependencies resolved with Gradle).
#
# Results: testbed/work/results/<cell>.log, JUnit XML in .../<cell>/
#
# (Written for bash 3.2, as shipped with macOS.)

set -u

gRoot=$(cd "$(dirname "$0")/.." && pwd)
gWork=$gRoot/testbed/work
gLib=$gWork/lib
gResults=$gWork/results
gSdkmanJava=${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java

gJavaMajors=(17 21 25)
# groovy group:version:minJava[:classifier]
gGroovys=(
  org.codehaus.groovy:2.5.23:8
  org.codehaus.groovy:2.5.23:8:indy
  org.codehaus.groovy:3.0.25:8
  org.codehaus.groovy:3.0.25:8:indy
  org.apache.groovy:4.0.33:8
  org.apache.groovy:5.1.3:11
  org.apache.groovy:6.0.0:17
)
gIvyVersion=2.5.3
gHamcrestVersion=3.0
gCommonsIoVersion=2.22.0
gJunitConsoleVersion=1.14.4

usage() {
  awk 'NR < 3 { next } /^#/ { sub(/^# ?/, ""); print; next } { exit }' "$0"
  exit "${1:-0}"
}

die() {
  echo "matrix.sh: $*" >&2
  exit 2
}

# comma list -> matches $2 exactly or as prefix (e.g. "4" matches "4.0.33")
inFilter() {
  local filter=$1 value=$2 item
  [[ -z $filter ]] && return 0
  local items
  IFS=, read -r -a items <<< "$filter"
  for item in "${items[@]}"; do
    [[ $value == "$item" || $value == "$item".* ]] && return 0
  done
  return 1
}

# version string -> sortable zero-padded key
versionKey() {
  local v=${1//_/.} key='' part parts
  IFS=.- read -r -a parts <<< "$v"
  for part in "${parts[@]:0:4}"; do
    [[ $part =~ ^[0-9]+$ ]] || part=0
    key=$key$(printf '%06d' "$((10#$part))")
  done
  printf '%s' "$key"
}

# java major -> JAVA_HOME (empty if none)
findJavaHome() {
  local major=$1 override best='' bestKey='' bestFx=1 dir release version vMajor key fx
  eval "override=\${MATRIX_JAVA_HOME_$major:-}"
  if [[ -n $override ]]; then
    printf '%s' "$override"
    return
  fi
  [[ -d $gSdkmanJava ]] || return
  for dir in "$gSdkmanJava"/*; do
    [[ -L $dir || ! -x $dir/bin/java ]] && continue
    release=$dir/release
    [[ -f $release ]] || continue
    version=$(sed -n 's/^JAVA_VERSION="\{0,1\}\([^"]*\)"\{0,1\}$/\1/p' "$release")
    [[ -z $version ]] && continue
    if [[ $version == 1.* ]]; then
      vMajor=${version#1.}
      vMajor=${vMajor%%.*}
    else
      vMajor=${version%%[.+_-]*}
    fi
    [[ $vMajor == "$major" ]] || continue
    fx=0
    [[ $(basename "$dir") == *fx* ]] && fx=1
    key=$(versionKey "$version")
    if [[ -z $best ]] || (( fx < bestFx )) || { (( fx == bestFx )) && [[ $key > $bestKey ]]; }; then
      best=$dir
      bestKey=$key
      bestFx=$fx
    fi
  done
  printf '%s' "$best"
}

# group:artifact:version[:classifier] -> path of jar in local cache
fetch() {
  local group=${1%%:*} rest=${1#*:}
  local artifact=${rest%%:*}
  rest=${rest#*:}
  local version=${rest%%:*} classifier=''
  [[ $rest == *:* ]] && classifier=-${rest#*:}
  local name=$artifact-$version$classifier.jar
  local file=$gLib/$name
  if [[ ! -f $file ]]; then
    local url="https://repo1.maven.org/maven2/${group//.//}/$artifact/$version/$name"
    curl -sfL -o "$file.part" "$url" || { rm -f "$file.part"; die "download failed: $url"; }
    mv "$file.part" "$file"
  fi
  printf '%s' "$file"
}

# group:artifact:version -> classpath incl. transitive dependencies (via Gradle)
resolve() {
  local coords=$1
  local file=$gLib/${coords//:/_}.classpath
  if [[ ! -f $file ]]; then
    local dir=$gWork/resolve
    mkdir -p "$dir"
    echo "rootProject.name = 'matrix-resolve'" > "$dir/settings.gradle"
    cat > "$dir/build.gradle" <<'EOF'
repositories { mavenCentral() }
def cp = configurations.create('cp')
dependencies { add('cp', providers.gradleProperty('coords').get()) }
tasks.register('printClasspath') {
  doLast { println cp.asPath }
}
EOF
    local javaHome='' major
    for major in 25 21 17; do
      javaHome=$(findJavaHome "$major")
      [[ -n $javaHome ]] && break
    done
    JAVA_HOME=${javaHome:-${JAVA_HOME:-}} "$gRoot/gradlew" -q -p "$dir" printClasspath \
      -Pcoords="$coords" > "$file.part" || { rm -f "$file.part"; die "resolving failed: $coords"; }
    mv "$file.part" "$file"
  fi
  tail -1 "$file"
}

# "N label" lines from the JUnit console summary
summaryCount() {
  local log=$1 label=$2
  sed -n "s/^.*\[ *\([0-9][0-9]*\) $label *\].*$/\1/p" "$log" | tail -1
}

gBuild=0
gJavaFilter=''
gGroovyFilter=''
while (( $# > 0 )); do
  case $1 in
    -b|--build) gBuild=1 ;;
    -j|--java) shift; gJavaFilter=${1:-} ;;
    -g|--groovy) shift; gGroovyFilter=${1:-} ;;
    -h|--help) usage ;;
    *) usage 2 ;;
  esac
  shift
done

cd "$gRoot" || die "cannot cd to $gRoot"

if (( gBuild )); then
  ./gradlew -q clean jar testClasses || die "gradle build failed"
fi

gMainJar=''
for f in build/libs/grengine-*.jar; do
  case $f in
    *-sources.jar|*-javadoc.jar|*-tests.jar) ;;
    *) [[ -f $f ]] && gMainJar=$gRoot/$f ;;
  esac
done
[[ -n $gMainJar ]] || die "no grengine jar in build/libs (use --build)"

gTestRoot=''
for f in build/libs/grengine-*-tests.jar; do
  [[ -f $f ]] && gTestRoot=$gRoot/$f
done
if [[ -z $gTestRoot ]]; then
  [[ -d build/classes/java/test ]] || die "no compiled tests (use --build)"
  gTestRoot=$gRoot/build/classes/java/test
fi

mkdir -p "$gLib" "$gResults" "$gWork/grapes"

gJunitJar=$(fetch "org.junit.platform:junit-platform-console-standalone:$gJunitConsoleVersion")
gCommonCp="$gMainJar:$gTestRoot:$(fetch "org.hamcrest:hamcrest:$gHamcrestVersion"):$(fetch "commons-io:commons-io:$gCommonsIoVersion")"

echo "main:  $gMainJar"
echo "tests: $gTestRoot"
echo

gFailedCells=0
gSummary=$gResults/summary.txt
: > "$gSummary"

runCell() {
  local id=$1 javaHome=$2 cp=$3
  shift 3
  local log=$gResults/$id.log
  rm -rf "${gResults:?}/$id"
  "$javaHome/bin/java" "$@" -Dgrape.root="$gWork/grapes" \
    -jar "$gJunitJar" execute \
    --disable-banner --details=summary \
    --class-path "$cp" --scan-classpath "$gTestRoot" --include-classname '.*' \
    --reports-dir "$gResults/$id" > "$log" 2>&1
  local status=$?
  local found succeeded failed skipped aborted
  found=$(summaryCount "$log" 'tests found')
  succeeded=$(summaryCount "$log" 'tests successful')
  failed=$(summaryCount "$log" 'tests failed')
  skipped=$(summaryCount "$log" 'tests skipped')
  aborted=$(summaryCount "$log" 'tests aborted')
  local result
  if (( status == 0 )); then
    result="OK      ${succeeded:-?}/${found:-?} passed, ${skipped:-0} skipped, ${aborted:-0} n/a"
  else
    result="FAILED  ${failed:-?}/${found:-?} failed (exit $status), see $log"
    gFailedCells=$((gFailedCells + 1))
  fi
  printf '%-34s %s\n' "$id" "$result" | tee -a "$gSummary"
}

for javaMajor in "${gJavaMajors[@]}"; do
  inFilter "$gJavaFilter" "$javaMajor" || continue
  javaHome=$(findJavaHome "$javaMajor")
  if [[ -z $javaHome ]]; then
    printf '%-34s %s\n' "java$javaMajor" "SKIPPED (no JDK found)" | tee -a "$gSummary"
    continue
  fi
  echo "java$javaMajor: $javaHome" | tee -a "$gSummary"
  for groovy in "${gGroovys[@]}"; do
    group=${groovy%%:*}
    rest=${groovy#*:}
    version=${rest%%:*}
    rest=${rest#*:}
    minJava=${rest%%:*}
    classifier=''
    [[ $rest == *:* ]] && classifier=${rest#*:}
    label=$version${classifier:+-$classifier}
    inFilter "$gGroovyFilter" "$label" || continue
    (( javaMajor < minJava )) && continue
    groovyMajor=${version%%.*}
    groovyCp=$(fetch "$group:groovy:$version${classifier:+:$classifier}")
    jvmArgs=()
    [[ $classifier == indy ]] && jvmArgs=(-Dgroovy.target.indy=true)

    if (( groovyMajor >= 6 )); then
      runCell "java$javaMajor-groovy$version-ivy" "$javaHome" \
        "$gCommonCp:$(resolve "$group:groovy-grape-ivy:$version")"
      runCell "java$javaMajor-groovy$version-maven" "$javaHome" \
        "$gCommonCp:$(resolve "$group:groovy-grape-maven:$version")"
    else
      ivyCp="$groovyCp:$(fetch "org.apache.ivy:ivy:$gIvyVersion")"
      runCell "java$javaMajor-groovy$label" "$javaHome" "$gCommonCp:$ivyCp" ${jvmArgs[@]+"${jvmArgs[@]}"}
    fi
  done
done

echo
echo "summary: $gSummary"
(( gFailedCells == 0 ))
