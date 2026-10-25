# Sample Garbage Collection Output

Garbage collection of released classes, recorded **25 Oct 2026**.

* `testbed/matrix.sh --gc` with Grengine 3 (JDK 8 bytecode).\
  (Results with Grengine 4 (JDK 17 bytecode) were similar for JDK 17/21/25.)
* Groovy 2.5/3/4/5/6 and JDK 8/11/17/21/25.
* Mac Studio (2023), Apple M2 Max ("ARM"/"Apple Silicon"), macOS Golden Gate 27.0.1.
* Note how fast Groovy 2.5 classes seem to load in comparison.
  I speculate maybe due to added features like more overloaded methods?

```
main:  $HOME/tech/idea/grengine/build/libs/grengine-3.2.1-SNAPSHOT.jar
gc:    20 secs per cell

java8: $HOME/.sdkman/candidates/java/8.0.504+1-zulu
java8-groovy2.5.23-gc              OK      gc: 35643 loaded, max 4029 alive, metaspace max 50.6m
java8-groovy2.5.23-indy-gc         OK      gc: 38613 loaded, max 2723 alive, metaspace max 33.3m
java8-groovy3.0.25-gc              OK      gc: 28850 loaded, max 3502 alive, metaspace max 49.2m
java8-groovy3.0.25-indy-gc         OK      gc: 30605 loaded, max 3939 alive, metaspace max 44.2m
java8-groovy4.0.33-gc              OK      gc: 31729 loaded, max 4195 alive, metaspace max 47.5m
java11: $HOME/.sdkman/candidates/java/11.0.32+1.1-zulu
java11-groovy2.5.23-gc             OK      gc: 39134 loaded, max 3273 alive, metaspace max 49.5m
java11-groovy2.5.23-indy-gc        OK      gc: 38373 loaded, max 4455 alive, metaspace max 48.8m
java11-groovy3.0.25-gc             OK      gc: 29229 loaded, max 3993 alive, metaspace max 60.8m
java11-groovy3.0.25-indy-gc        OK      gc: 31178 loaded, max 3821 alive, metaspace max 49.5m
java11-groovy4.0.33-gc             OK      gc: 32303 loaded, max 3957 alive, metaspace max 51.4m
java11-groovy5.1.3-gc              OK      gc: 28172 loaded, max 3532 alive, metaspace max 51.9m
java17: $HOME/.sdkman/candidates/java/17.0.20+1.1-zulu
java17-groovy2.5.23-gc             OK      gc: 48073 loaded, max 3002 alive, metaspace max 39.0m
java17-groovy2.5.23-indy-gc        OK      gc: 48423 loaded, max 2523 alive, metaspace max 28.8m
java17-groovy3.0.25-gc             OK      gc: 35265 loaded, max 2759 alive, metaspace max 40.9m
java17-groovy3.0.25-indy-gc        OK      gc: 35017 loaded, max 2395 alive, metaspace max 31.9m
java17-groovy4.0.33-gc             OK      gc: 38229 loaded, max 2019 alive, metaspace max 30.7m
java17-groovy5.1.3-gc              OK      gc: 33876 loaded, max 2433 alive, metaspace max 35.2m
java17-groovy6.0.0-gc              OK      gc: 31523 loaded, max 2490 alive, metaspace max 37.3m
java21: $HOME/.sdkman/candidates/java/21.0.12+1.1-zulu
java21-groovy2.5.23-gc             OK      gc: 47674 loaded, max 2781 alive, metaspace max 37.9m
java21-groovy2.5.23-indy-gc        OK      gc: 48871 loaded, max 1985 alive, metaspace max 25.7m
java21-groovy3.0.25-gc             OK      gc: 35248 loaded, max 1467 alive, metaspace max 29.9m
java21-groovy3.0.25-indy-gc        OK      gc: 35968 loaded, max 1406 alive, metaspace max 25.7m
java21-groovy4.0.33-gc             OK      gc: 38291 loaded, max 1612 alive, metaspace max 28.3m
java21-groovy5.1.3-gc              OK      gc: 34231 loaded, max 1711 alive, metaspace max 30.6m
java21-groovy6.0.0-gc              OK      gc: 32045 loaded, max 1236 alive, metaspace max 28.5m
java25: $HOME/.sdkman/candidates/java/25.0.4+1.1-zulu
java25-groovy2.5.23-gc             OK      gc: 48958 loaded, max 2225 alive, metaspace max 32.3m
java25-groovy2.5.23-indy-gc        OK      gc: 51017 loaded, max 2045 alive, metaspace max 25.7m
java25-groovy3.0.25-gc             OK      gc: 36269 loaded, max 1523 alive, metaspace max 29.7m
java25-groovy3.0.25-indy-gc        OK      gc: 37324 loaded, max 1865 alive, metaspace max 28.2m
java25-groovy4.0.33-gc             OK      gc: 40031 loaded, max 1678 alive, metaspace max 28.1m
java25-groovy5.1.3-gc              OK      gc: 35406 loaded, max 1883 alive, metaspace max 31.1m
java25-groovy6.0.0-gc              OK      gc: 32627 loaded, max 1574 alive, metaspace max 30.2m
```
