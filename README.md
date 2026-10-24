[![image](grengine.jpg)](https://grengine.ch)

Grengine is an engine for running and embedding Groovy in a Java VM.

See [user manual](MANUAL.md) for distinguishing features.

Homepage: [grengine.ch](https://grengine.ch/) (Javadoc and more)

Overview:
* **Grengine 4: JDK 17+** → **active** (`main` branch)
* **Grengine 3: JDK 8+** (Groovy 6 supported with JDK 17+) → **active** (`grengine3` branch)
* Grengine 2: JDK 8 (or later) → no new releases planned
* Grengine 1: JDK 6 (or later) → no new releases planned
* Grengine 3+4 features are kept in sync
  and are tested with a matrix of Groovy 6/5/4/3/2.5 x JDK 25/21/17/11/8 for each release.
* API changes have been minimal and essentially non-breaking since Grengine 1.0.0 (2014).

Release notes are at the bottom of the [user manual](MANUAL.md#release-notes).

Build: `./gradlew clean all`

License: Apache 2.0.
