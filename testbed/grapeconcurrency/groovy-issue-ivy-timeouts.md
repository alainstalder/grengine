**Link:** (after creating) https://issues.apache.org/jira/browse/GROOVY-12640

**Title:** Grape (Ivy): default grapeConfig.xml sets no connection/read timeouts, so a stalled download hangs @Grab forever

**Component:** Grape (groovy-grape-ivy) · **Affects:** 6.0.0 (same with 3.0.25, 4.0.33, 5.1.3)

---

Groovy's built-in Ivy settings (`groovy/grape/ivy/defaultGrapeConfig.xml` in groovy-grape-ivy,
`groovy/grape/defaultGrapeConfig.xml` in earlier versions) do not configure any timeouts.
Ivy's URL handler then uses the JDK default for `HttpURLConnection`, i.e. no connection or
read timeout. If a repository stops sending data in the middle of a transfer without closing
the connection, the grab never returns.

Because the default settings use `lockStrategy="artifact-lock-nio"`, it does not stay with
the one stuck thread. Every other thread in the JVM (or other process sharing the Grape
cache) that needs the same artifact waits forever in `FileBasedLockStrategy.acquireLock`.

We hit this when running many concurrent grabs against Maven Central (guava 10.0–19.0,
100 threads, empty cache). Runs sometimes hung with all threads for one version stuck:
one in a socket read, the rest in `acquireLock`. A local HTTP server that stalls responses
reproduces exactly that, and adding an Ivy timeout constraint fixes it.

**Reproduction**

The attached `GrapeStall.groovy` starts a local HTTP Maven repository on 127.0.0.1. The repository
serves the POM of `repro:stall:1.0` normally, but sends only half of the jar and then stalls.
The script then writes a minimal Ivy settings file (the same `settings`/`caches`/`ibiblio` setup as
the default config, with the ibiblio root pointing to the local server), sets `grape.config`
and `grape.root` to a temp directory, and grabs the artifact from two threads.

```
groovy GrapeStall.groovy      # settings as default: no timeouts
groovy GrapeStall.groovy 5    # plus timeout-constraint, 5 s connection/read timeout
```

Without timeouts (Groovy 6.0.0, groovy-grape-ivy, Ivy 2.6.0, Java 21):

```
Groovy 6.0.0, Java 21.0.13, timeout: none
server: stalling GET /repro/stall/1.0/stall-1.0.jar

grab-1 still running after 30s, at:
    at java.base/sun.nio.ch.SocketDispatcher.read0(Native Method)
    ...
    at java.base/sun.net.www.MeteredStream.read(MeteredStream.java:131)
    ...
grab-2 still running after 30s, at:
    at java.base/java.lang.Thread.sleep(Thread.java:509)
    at org.apache.ivy.plugins.lock.FileBasedLockStrategy.acquireLock(FileBasedLockStrategy.java:105)
    at org.apache.ivy.plugins.lock.ArtifactLockStrategy.lockArtifact(ArtifactLockStrategy.java:32)
    at org.apache.ivy.core.cache.DefaultRepositoryCacheManager.lockMetadataArtifact(DefaultRepositoryCacheManager.java:1448)
    ...

RESULT: 2 of 2 grab threads hung
```

(They stay like that indefinitely; the script just stops looking after 30 s.)

With a 5 s timeout constraint, the stalled download fails with `Read timed out (5011ms)`.
The second thread then gets the lock, tries again, and also fails after 5 s. Both grabs end with
`download failed: repro#stall;1.0!stall.jar`, and there are no hangs (~11 s in total).

The results are the same with Groovy 3.0.25 and 4.0.33 (Ivy 2.5.3) and 5.1.3 (Ivy 2.6.0).

**Proposal**

Add a timeout constraint to the default settings (and to `relaxedGrapeConfig.xml`), and
reference it from the remote `ibiblio` resolver. Timeout constraints have been supported
since Ivy 2.5.0.

```xml
<ivysettings>
  <settings defaultResolver="downloadGrapes"/>
  <caches lockStrategy="artifact-lock-nio"/>
  <timeout-constraints>
    <timeout-constraint name="grape" connectionTimeout="30000" readTimeout="60000"/>
  </timeout-constraints>
  ...
      <ibiblio name="ibiblio" m2compatible="true" timeoutConstraint="grape"/>
  ...
</ivysettings>
```

The values are only a suggestion. The read timeout applies to each socket read, not to the
whole transfer, so even generous values do not limit large downloads. They only turn
"hangs forever" into a normal "download failed" error. Users with their own
`~/.groovy/grapeConfig.xml` or `grape.config` are not affected, since such a file replaces the
default instead of being merged with it. That is also the current workaround: copy the whole
default config and add the lines above.

The same change would apply unchanged to the 4.0.x and 5.x defaults, if a backport is wanted.

(Not covered here: groovy-grape-maven uses Maven Resolver's own transport timeouts.)
