Grengine
========

Grengine is an engine for running and embedding Groovy in a Java VM.

Fast, easy-to-use and highly customizable.

Uses only the compiler from the Groovy JDK, not the `GroovyClassLoader`*, `GroovyShell` or `GroovyScriptEngine`.

See [grengine.ch](http://www.grengine.ch/) for javadoc, downloads and more.

Features
--------

* Works essentially with any Groovy version (1.7.5 or later).
* Very robust towards API changes in the Groovy JDK in future Groovy versions, because it only uses the Groovy compiler (including a CompilerConfiguration) - a GroovyClassLoader is only marginally used internally during compilation.
* High performance without much ado, recompilations only if really necessary.
* Automatic code updates if sources change or static code, multiple layers of code, multiple identical parallel class loaders (for full separation of static variables, e.g. between different user sessions), different class loading strategies (parent or current first), and much more...
* Highly configurable and extensible.

Building
--------

* Requires Java 6 (or later) and Gradle
* Quick build: `gradle clean build`
* Full build: `gradle clean build pom jacoco`
* Create eclipse project: `gradle eclipse`

Usage
-----

A user manual may be provided in the future; for the moment:

* See the javadoc of `Grengine` and `BaseGrengine` for how to run/create/load scripts.
* See all interfaces, like `Source`, `Sources`, etc. to get a grasp of the concepts.
* Check out all the Builder classes for options when creating objects.
* See the source code and especially all the unit tests for how it is used.
* Try it out!

License
-------

    Copyright 2014-now by Alain Stalder. Made in Switzerland.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

&copy; 2014-now by Alain Stalder. Made in Switzerland.
