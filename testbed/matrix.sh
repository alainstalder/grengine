#!/usr/bin/env bash
#
# Initially created by Claude Code (Opus 5.5) in Oct 2026.
#
# Runs the Grengine unit tests against a matrix of Java and Groovy versions,
# using the jars built by Gradle (no changes to the Gradle build needed).
#
# Usage: testbed/matrix.sh [-b|--build] [-j|--java 17,25] [-g|--groovy 4,6.0.0]
#                          [--gc] [--gc-secs 20]
#                          [--grape] [--grape-http|--grape-central] [--grape-runs 1]
#                          [--grape-http-delay 0] [--grape-http-faults 0]
#                          [--grape-ivy-timeout 0]
#
#   -b, --build    run './gradlew clean jar testClasses' first
#   -j, --java     comma-separated Java majors (default: 17,21,25)
#   -g, --groovy   comma-separated Groovy versions or version prefixes
#                  (default: 2.5.23,2.5.23-indy,3.0.25,3.0.25-indy,
#                  4.0.33,5.1.3,6.0.0)
#   --gc           run the class garbage collection check
#                  (testbed/classgc/GrengineClassGc.java) instead of
#                  the unit tests, with -XX:MaxMetaspaceSize=64m
#   --gc-secs      seconds per cell for --gc (default: 20)
#   --grape        run the Grape concurrency check (GROOVY-7407,
#                  testbed/grapeconcurrency/GrapeConcurrency.java) instead
#                  of the unit tests, once without and once with Grengine
#                  lock; without lock, failures are only reported as INFO
#   --grape-central  grab from Maven Central instead of from a local Maven
#                  repository (testbed/work/grape-repo/, set up on demand)
#   --grape-http   serve the local Maven repository over HTTP from 127.0.0.1
#                  instead of as file: URLs
#   --grape-http-delay  milliseconds to delay each HTTP response and each
#                  16 KB sent, like a slow network (default: 0, implies --grape-http)
#   --grape-http-faults  percentage of HTTP responses that fail, half with
#                  status 503 and half aborted after half of the body
#                  (default: 0, implies --grape-http)
#   --grape-ivy-timeout  Ivy connection and read timeout in seconds
#                  (default: 0, no timeout, as in the Groovy default config)
#   --grape-runs   runs per cell for --grape, results are summed up
#                  (default: 1)
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
# (with --gc: testbed/work/results/<cell>-gc.log,
# with --grape: testbed/work/results/<cell>-lock.log and <cell>-nolock.log)
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
gIvyVersion=2.6.0
gHamcrestVersion=3.0
gCommonsIoVersion=2.22.0
gJunitConsoleVersion=6.1.3

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

# group artifact version ext -> file in local Maven repo for --grape (incl. .sha1)
mirror() {
  local group=$1 artifact=$2 version=$3 ext=$4
  local path=${group//.//}/$artifact/$version/$artifact-$version.$ext
  local file=$gGrapeRepo/$path suffix
  for suffix in '' .sha1; do
    if [[ ! -f $file$suffix ]]; then
      mkdir -p "$(dirname "$file")"
      local url="https://repo1.maven.org/maven2/$path$suffix"
      curl -sfL -o "$file$suffix.part" "$url" || { rm -f "$file$suffix.part"; die "download failed: $url"; }
      mv "$file$suffix.part" "$file$suffix"
    fi
  done
}

# group artifact version -> POM plus parent POMs in local Maven repo
mirrorPom() {
  mirror "$1" "$2" "$3" pom
  local parent
  parent=$(tr -d ' \t\r\n' < "$gGrapeRepo/${1//.//}/$2/$3/$2-$3.pom" | sed -n \
    's|.*<parent><groupId>\([^<]*\)</groupId><artifactId>\([^<]*\)</artifactId><version>\([^<]*\)</version>.*|\1 \2 \3|p')
  if [[ -n $parent ]]; then
    mirrorPom $parent
  fi
}

# "N label" lines from the JUnit console summary
summaryCount() {
  local log=$1 label=$2
  sed -n "s/^.*\[ *\([0-9][0-9]*\) $label *\].*$/\1/p" "$log" | tail -1
}

gBuild=0
gJavaFilter=''
gGroovyFilter=''
gGc=0
gGcSecs=20
gGrape=0
gGrapeCentral=0
gGrapeHttp=0
gGrapeHttpDelay=0
gGrapeHttpFaults=0
gGrapeIvyTimeout=0
gGrapeRuns=1
while (( $# > 0 )); do
  case $1 in
    -b|--build) gBuild=1 ;;
    -j|--java) shift; gJavaFilter=${1:-} ;;
    -g|--groovy) shift; gGroovyFilter=${1:-} ;;
    --gc) gGc=1 ;;
    --gc-secs) shift; gGcSecs=${1:-} ;;
    --grape) gGrape=1 ;;
    --grape-central) gGrape=1; gGrapeCentral=1 ;;
    --grape-http) gGrape=1; gGrapeHttp=1 ;;
    --grape-http-delay) shift; gGrape=1; gGrapeHttp=1; gGrapeHttpDelay=${1:-} ;;
    --grape-http-faults) shift; gGrape=1; gGrapeHttp=1; gGrapeHttpFaults=${1:-} ;;
    --grape-ivy-timeout) shift; gGrape=1; gGrapeIvyTimeout=${1:-} ;;
    --grape-runs) shift; gGrape=1; gGrapeRuns=${1:-} ;;
    -h|--help) usage ;;
    *) usage 2 ;;
  esac
  shift
done
[[ $gGcSecs =~ ^[0-9]+$ ]] || die "--gc-secs requires a number of seconds"
[[ $gGrapeRuns =~ ^[1-9][0-9]*$ ]] || die "--grape-runs requires a number of runs"
[[ $gGrapeHttpDelay =~ ^[0-9]+$ ]] || die "--grape-http-delay requires a number of milliseconds"
[[ $gGrapeHttpFaults =~ ^[0-9]+$ ]] && (( gGrapeHttpFaults <= 100 )) || die "--grape-http-faults requires a percentage"
[[ $gGrapeIvyTimeout =~ ^[0-9]+$ ]] || die "--grape-ivy-timeout requires a number of seconds"
(( gGc && gGrape )) && die "--gc and --grape cannot be combined"
(( gGrapeCentral && gGrapeHttp )) && die "--grape-central and --grape-http cannot be combined"
gGrapeRepo=$gWork/grape-repo

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

mkdir -p "$gLib" "$gResults" "$gWork/grapes"

echo "main:  $gMainJar"
if (( gGc )); then
  echo "gc:    $gGcSecs secs per cell"
elif (( gGrape )); then
  if (( gGrapeCentral )); then
    echo "grape: from Maven Central, $gGrapeRuns run(s) per cell"
  else
    for v in 10 11 12 13 14 15 16 17 18 19; do
      mirrorPom com.google.guava guava "$v.0"
      mirror com.google.guava guava "$v.0" jar
    done
    mirrorPom com.google.code.findbugs jsr305 1.3.9
    mirror com.google.code.findbugs jsr305 1.3.9 jar
    echo "grape: from $gGrapeRepo$( (( gGrapeHttp )) && printf " over HTTP"; (( gGrapeHttpDelay )) && printf " (delay %s ms)" "$gGrapeHttpDelay"; (( gGrapeHttpFaults )) && printf " (faults %s%%)" "$gGrapeHttpFaults"), $gGrapeRuns run(s) per cell"
  fi
  if (( gGrapeIvyTimeout )); then
    echo "grape: Ivy timeout $gGrapeIvyTimeout secs"
  fi
else
  gTestRoot=''
  for f in build/libs/grengine-*-tests.jar; do
    [[ -f $f ]] && gTestRoot=$gRoot/$f
  done
  if [[ -z $gTestRoot ]]; then
    [[ -d build/classes/java/test ]] || die "no compiled tests (use --build)"
    gTestRoot=$gRoot/build/classes/java/test
  fi
  gJunitJar=$(fetch "org.junit.platform:junit-platform-console-standalone:$gJunitConsoleVersion")
  gCommonCp="$gMainJar:$gTestRoot:$(fetch "org.hamcrest:hamcrest:$gHamcrestVersion"):$(fetch "commons-io:commons-io:$gCommonsIoVersion")"
  echo "tests: $gTestRoot"
fi
echo

gFailedCells=0
gCells=0
gSummary=$gResults/summary$( (( gGc )) && echo -gc; (( gGrape )) && echo -grape ).txt
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
    result="OK      ${succeeded:-?}/${found:-?} passed"
    if (( ${skipped:-0} > 0 )); then
      result+=", $skipped skipped"
    fi
    if (( ${aborted:-0} > 0 )); then
      result+=", $aborted n/a"
    fi
  else
    result="FAILED  ${failed:-?}/${found:-?} failed (exit $status), see $log"
    gFailedCells=$((gFailedCells + 1))
  fi
  printf '%-34s %s\n' "$id" "$result" | tee -a "$gSummary"
  gCells=$((gCells + 1))
}

runGcCell() {
  local id=$1 javaHome=$2 cp=$3
  shift 3
  local log=$gResults/$id.log
  local classes=$gWork/classgc/$id
  rm -rf "$classes"
  mkdir -p "$classes"
  local status
  if "$javaHome/bin/javac" -nowarn -d "$classes" -cp "$cp" \
      "$gRoot/testbed/classgc/GrengineClassGc.java" > "$log" 2>&1; then
    "$javaHome/bin/java" "$@" -XX:MaxMetaspaceSize=64m -Xmx256m \
      -cp "$classes:$cp" GrengineClassGc -secs "$gGcSecs" >> "$log" 2>&1
    status=$?
  else
    status=$?
  fi
  local gcResult result
  gcResult=$(sed -n 's/^RESULT //p' "$log" | tail -1)
  if (( status == 0 )); then
    result="OK      gc: ${gcResult#OK }"
  else
    result="FAILED  gc: ${gcResult:-no result} (exit $status), see $log"
    gFailedCells=$((gFailedCells + 1))
  fi
  printf '%-34s %s\n' "$id" "$result" | tee -a "$gSummary"
  gCells=$((gCells + 1))
}

runGrapeCell() {
  local id=$1 javaHome=$2 cp=$3 lock=$4
  shift 4
  local log=$gResults/$id.log
  local dir=$gWork/grapeconcurrency/$id
  rm -rf "$dir"
  mkdir -p "$dir/classes"
  local toolArgs=(-ivyTimeout "$gGrapeIvyTimeout")
  if (( lock )); then
    toolArgs+=(-lock)
  fi
  if (( ! gGrapeCentral )); then
    toolArgs+=(-repo "$gGrapeRepo")
    if (( gGrapeHttp )); then
      toolArgs+=(-httpDelay "$gGrapeHttpDelay" -httpFaults "$gGrapeHttpFaults")
    fi
  fi
  local result
  if ! "$javaHome/bin/javac" -nowarn -d "$dir/classes" -cp "$cp" \
      "$gRoot/testbed/grapeconcurrency/GrapeConcurrency.java" > "$log" 2>&1; then
    result="FAILED  grape: could not compile, see $log"
    gFailedCells=$((gFailedCells + 1))
    printf '%-34s %s\n' "$id" "$result" | tee -a "$gSummary"
    gCells=$((gCells + 1))
    return
  fi
  # sums over runs; a run without RESULT line (e.g. crashed) counts as broken
  local run runLog status ok failed hung nOk=0 nFailed=0 nHung=0 failedRuns=0 brokenRuns=0
  for (( run = 1; run <= gGrapeRuns; run++ )); do
    runLog=$dir/run$run.log
    "$javaHome/bin/java" "$@" -cp "$dir/classes:$cp" GrapeConcurrency \
      -root "$dir/root$run" ${toolArgs[@]+"${toolArgs[@]}"} > "$runLog" 2>&1
    status=$?
    { echo "=== run $run/$gGrapeRuns"; cat "$runLog"; } >> "$log"
    if (( status != 0 )); then
      failedRuns=$((failedRuns + 1))
    fi
    ok='' failed='' hung=''
    read -r ok failed hung <<< "$(sed -n \
      's/^RESULT [A-Z]* \([0-9]*\) ok, \([0-9]*\) failed, \([0-9]*\) hung.*$/\1 \2 \3/p' "$runLog")"
    if [[ -z $hung ]] || (( status != 0 && status != 1 )); then
      brokenRuns=$((brokenRuns + 1))
    else
      nOk=$((nOk + ok))
      nFailed=$((nFailed + failed))
      nHung=$((nHung + hung))
    fi
  done
  local summary="$nOk ok, $nFailed failed, $nHung hung"
  if (( gGrapeRuns > 1 )); then
    summary="$gGrapeRuns runs ($failedRuns failed), $summary"
  fi
  if (( failedRuns == 0 )); then
    result="OK      grape: $summary"
  elif (( ! lock && brokenRuns == 0 )); then
    result="INFO    grape: $summary"
  else
    result="FAILED  grape: $summary, see $log"
    gFailedCells=$((gFailedCells + 1))
  fi
  printf '%-34s %s\n' "$id" "$result" | tee -a "$gSummary"
  gCells=$((gCells + 1))
}

# without and with Grengine lock
runGrapeCells() {
  local id=$1 javaHome=$2 cp=$3
  shift 3
  runGrapeCell "$id-nolock" "$javaHome" "$cp" 0 "$@"
  runGrapeCell "$id-lock" "$javaHome" "$cp" 1 "$@"
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

    if (( gGc )); then
      runGcCell "java$javaMajor-groovy$label-gc" "$javaHome" "$gMainJar:$groovyCp" \
        ${jvmArgs[@]+"${jvmArgs[@]}"}
    elif (( gGrape && groovyMajor >= 6 )); then
      runGrapeCells "java$javaMajor-groovy$version-ivy" "$javaHome" \
        "$gMainJar:$(resolve "$group:groovy-grape-ivy:$version")"
      runGrapeCells "java$javaMajor-groovy$version-maven" "$javaHome" \
        "$gMainJar:$(resolve "$group:groovy-grape-maven:$version")"
    elif (( gGrape )); then
      runGrapeCells "java$javaMajor-groovy$label" "$javaHome" \
        "$gMainJar:$groovyCp:$(fetch "org.apache.ivy:ivy:$gIvyVersion")" ${jvmArgs[@]+"${jvmArgs[@]}"}
    elif (( groovyMajor >= 6 )); then
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

if (( gCells == 0 )); then
  echo "no cells selected (Java majors: ${gJavaMajors[*]}, see --help)" | tee -a "$gSummary"
  gFailedCells=1
fi

echo
echo "summary: $gSummary"
(( gFailedCells == 0 ))
