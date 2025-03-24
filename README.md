> _"I am here to provide care, not just as a robot, but as a friend"_

_**Baymax**, Big Hero 6_

# baymax <img src="doc/img/baymax-logo.png" width="70px">
[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Fbaymax%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/baymax/releases)

* **collects** intel from an arbitrary **sources**
* **transforms** them
* **serves** them
* and **publishes** them to arbitrary destinations

![image](https://github.com/user-attachments/assets/d8a1b8cd-9800-4faf-a08e-002d43f10123)


## run it

```bash
$ make image
$ ## make your ".env" with overrides: hosts, ports, secrets, etc.
$ export BAYMAX_CONFIG=path-to-baymax.edn; make run

092dd762c39eea90a80e4a812c7ec32dc666f66a36a1b3394083674a0dee612a
baymax container is up and ready to rock & roll
```

up and running:

```
http://localhost:4242/health
http://localhost:4242/intel-all
...
```

## license

Copyright © 2025 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

