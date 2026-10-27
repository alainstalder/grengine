[![image](grengine.jpg)](https://grengine.ch)

Grengine is an engine for running and embedding Groovy in a Java VM.

Homepage: [grengine.ch](https://grengine.ch/)

Key features compared to what standard Groovy classes provide:

* **Same scripts are automatically only compiled once**
  already in basic usage (`Grengine` vs. `GroovyShell`).
* Improved **thread-safety and more** when **grabbing** Maven dependencies
  (`@Grab`) while compiling a script.
* **Powerful features for robust and performant containers**,
  including the unique (afaik) feature of **caching compiled bytecode**
  so that reuse in different classloaders requires no re-compilation,
  which also allows for much stronger **session separation**.

See the [user manual](MANUAL.md) for an introduction
to some Groovy basics and the documentation of Grengine's features
from simple usage (`new Grengine().run("println 'Hello!'")`)
to complex container scenarios, all with examples.

### Overview

* **Grengine 4: JDK 17+** → **active** (`main` branch)
* **Grengine 3: JDK 8+** (Groovy 6 supported with JDK 17+) → **active** (`grengine3` branch)
* Grengine 2: JDK 8+ → no new releases planned.
* Grengine 1: JDK 6+ → no new releases planned.
* Grengine 3+4 features are kept in sync
  and are tested with a matrix of Groovy 6/5/4/3/2.5 x JDK 25/21/17/11/8 for each release.
* More details in the [release notes at the bottom of the user manual](MANUAL.md#release-notes).
* API changes have been minimal and essentially non-breaking since Grengine 1.0.0 (2014).
* Apache 2.0 License.
* Build with Gradle: `./gradlew clean all`
* Links:
  [Maven Central](https://central.sonatype.com/namespace/ch.grengine)
  • [Javadoc](https://grengine.ch/javadoc/)
  • [JaCoCo](https://grengine.ch/jacoco/)
