> _"I am here to provide care, not just as a robot, but as a friend"_

_**Baymax**, Big Hero 6_

# baymax <img src="doc/img/baymax-logo.png" width="70px">
[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Fbaymax%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/baymax/releases)

* **collects** intel from arbitrary **sources**
* **transforms** them
* **serves** them
* and **publishes** them to arbitrary destinations

![image](https://github.com/user-attachments/assets/d8a1b8cd-9800-4faf-a08e-002d43f10123)


## run it

### docker

```bash
$ make image
$ ## make your ".env" with overrides: hosts, ports, secrets, etc.
$ export BAYMAX_CONFIG=path-to-baymax-config.edn; make run

092dd762c39eea90a80e4a812c7ec32dc666f66a36a1b3394083674a0dee612a
baymax container is up and ready to rock & roll
```

up and running:

```
http://localhost:4242/health
http://localhost:4242/intel-all
...
```

![image](https://github.com/user-attachments/assets/b8eab038-ab42-441b-b998-8701f61ce649)

or run it as an uberjar without docker..

### uberjar

override the [baymax config](https://github.com/tolitius/baymax/blob/master/dev/resources/simple-config.edn) with secrets / env jawns:

```bash
export SOURCES__SOME_DB__CONNECTION__HOST=...
export SOURCES__SOME_DB__CONNECTION__PORT=...
export SOURCES__SOME_DB__CONNECTION__DATABASE=...
export SOURCES__SOME_DB__CONNECTION__USER=...
export SOURCES__SOME_DB__CONNECTION__PASSWORD=...
```

> [!TIP]
> _syntax follows the [way of cprop](https://github.com/tolitius/cprop?tab=readme-ov-file#speaking-env-variables)_ <img src="https://github.com/user-attachments/assets/c16f764f-1dc4-48fc-ac4b-370fd931a60e" width="32px"/>

build the jar:

```bash
$ make jar
```

run it:

```bash
java -Dconf=<path to config.edn> -jar target/baymax-standalone.jar
```

## license

Copyright © 2025 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

