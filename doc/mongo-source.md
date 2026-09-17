# mongo source

> _back to the [README](../README.md)_

collect from mongo, print intel to stdout, expose it in prometheus format.
nothing to build: [the image](https://hub.docker.com/r/tolitius/baymax) is on docker hub.

## step 1: pull it and make sure it runs

```bash
docker pull tolitius/baymax:latest
curl -sLO https://raw.githubusercontent.com/tolitius/baymax/master/dev/resources/mongo-config.edn

docker run -d --name baymax -p 4242:4242 \
  -v $PWD/mongo-config.edn:/opt/app/baymax/config.edn \
  tolitius/baymax:latest

curl -s localhost:4242/health      ## {"status":"6 feet above", ...}
curl -s localhost:4242/dashboard   ## "cosmos-mongo", "type":"mongo"
docker logs -f baymax
open http://localhost:4242/        ## the dashboard
```

there is no mongo to talk to yet, hence the source will say `"healthy": false` with a
"connection refused": baymax itself is up and 6 feet above.

## step 2: point it to mongodb

```bash
curl -sLO https://raw.githubusercontent.com/tolitius/baymax/master/dev/resources/mongo.env.sample
mv mongo.env.sample baymax.env    ## and fill it in

docker rm -f baymax
docker run -d --name baymax -p 4242:4242 \
  --env-file baymax.env \
  -v $PWD/mongo-config.edn:/opt/app/baymax/config.edn \
  tolitius/baymax:latest
```

```bash
SOURCES__COSMOS_MONGO__CONNECTION__URL=mongodb+srv://cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&tls=true
SOURCES__COSMOS_MONGO__CONNECTION__USER=astro_reader
SOURCES__COSMOS_MONGO__CONNECTION__PASSWORD=super-secret
SOURCES__COSMOS_MONGO__CONNECTION__DATABASE=cosmos
SOURCES__COSMOS_MONGO__CONNECTION__AUTH_SOURCE=admin
```

> [!IMPORTANT]
> _keep credentials out of the url: a mongo source takes them as `:user` / `:password`,
> so the url is free to carry the interesting things: cluster (srv) seed list, tls, replica set, timeouts, etc._

behind a private certificate authority? the mongo java driver does not read a `tlsCAFile` from the url
(that one is a `mongosh` option), it trusts whatever the jvm trust store trusts, and an unknown ca ends in

```
javax.net.ssl.SSLHandshakeException: PKIX path building failed ... unable to find valid certification path
```

point the source at the pem instead, and mount it:

```bash
SOURCES__COSMOS_MONGO__CONNECTION__TLS_CA_FILE=/opt/app/baymax/root-ca.pem
```

```bash
docker run -d --name baymax -p 4242:4242 \
  --env-file baymax.env \
  -v $PWD/root-ca.pem:/opt/app/baymax/root-ca.pem \
  -v $PWD/mongo-config.edn:/opt/app/baymax/config.edn \
  tolitius/baymax:latest
```

trust is then built from that pem for this source only: the jvm trust store is left alone.
a `tlsCAFile` that is already in the url is picked up as well, since urls tend to come from a
`mongosh` command that works

a couple of things that are good to know:

* an env var can only override a key that is **already** in the config file, so keep the placeholders
* `__` is a nesting level, a single `_` becomes a `-`: `..__AUTH_SOURCE` => `[... :connection :auth-source]`
* values are coerced: all digits become a number, `true` / `false` become a boolean
* if mongo runs on _this_ host, rather than in a container, it is `host.docker.internal:27017`
  (docker desktop) or `--network host` (linux): inside a container `localhost` is the container

## step 3: look at the intel

```bash
docker logs -f baymax                                              ## pretty printed, every minute
curl -s -XPOST localhost:4242/collector/collect/asteroid-metrics   ## .. or collect right now
curl -s localhost:4242/intel/asteroid-metrics                      ## json
curl -s localhost:4242/intel-all/prometheus                        ## prometheus
```

pretty print is for human eyes, log shippers like json better: one object per metric, one per line

```clojure
:logging
{:metrics {:format :json}}   ;; :pretty (default) or :json
```

```bash
LOGGING__METRICS__FORMAT=json    ## .. or from the environment
```

each metric becomes its own log event, hence it carries the usual time / thread / level prefix

```
2026-01-17T14:02:00,659 [pool-2-thread-1] INFO  baymax.publisher.stdout - publishing intel: {"collector":"asteroid-metrics","at":"2026-01-17 14:02:00","intel":{"name":"asteroids.count","value":3,"labels":{"belt":"main"}}}
```

```bash
docker logs -f baymax | grep -o '{"collector".*}' | jq .   ## .. the json out of it
```

```
# HELP asteroids_count Metric: asteroids_count
# TYPE asteroids_count gauge
asteroids_count{belt="kuiper",} 1.0
asteroids_count{belt="main",} 3.0
```

point prometheus to it:

```yaml
scrape_configs:
  - job_name: baymax
    metrics_path: /intel-all/prometheus
    static_configs:
      - targets: ['localhost:4242']
```

> [!TIP]
> _a collector shows up in `/intel-all/prometheus` when it is listed under a `:prometheus` publisher
> in [the config](https://github.com/tolitius/baymax/blob/master/dev/resources/mongo-config.edn)_
