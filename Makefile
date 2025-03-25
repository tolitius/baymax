.PHONY: clean jar tag image run outdated install deploy tree test repl

clean:
	rm -rf target
	rm -rf .env

jar: tag
	rm -rf target && mkdir target
	clojure -X:uberjar :jar target/baymax-standalone.jar :main-class baymax.app

outdated:
	clojure -M:outdated

tag:
	clojure -A:tag

## make image
## make image VERSION=0.0.42
image: jar
	docker build -t baymax:$${VERSION:-latest} .

BAYMAX_PORT ?= 4242

run:
	@if [ -z "$(BAYMAX_CONFIG)" ]; then \
		echo "error: BAYMAX_CONFIG is not set. please set the BAYMAX_CONFIG environment variable to a path of baymax configuration file."; \
		exit 1; \
	fi
	@if [ ! -f "$(BAYMAX_CONFIG)" ]; then \
		echo "error: config file at $(BAYMAX_CONFIG) does not exist"; \
		exit 1; \
	fi
	@if [ ! -f .env ]; then \
		echo "error: '.env' file is missing. it needs to have env related: network, secrets, etc. overrides for baymax config."; \
		exit 1; \
	fi
	@docker_run="docker run -d --env-file .env -v ${BAYMAX_CONFIG}:/opt/app/baymax/config.edn"; \
	if [ -n "$(BAYMAX_PORT)" ]; then \
		docker_run="$$docker_run -p $(BAYMAX_PORT):4242"; \
	fi; \
	$$docker_run baymax:$${VERSION:-latest}
	@echo "baymax container is up and ready to rock & roll"

push:
	@docker login
	@docker tag baymax:$${VERSION:-latest} tolitius/baymax:$${VERSION:-latest}
	@docker push tolitius/baymax:$${VERSION:-latest}
	@echo "baymax image pushed to docker hub: tolitius/baymax:$${VERSION:-latest}"

install: jar
	clojure -A:install

deploy: jar
	clojure -A:deploy

tree:
	mvn dependency:tree

test:
	clojure -X:test :patterns '[".*test.*"]'

## does not work with "-M"s ¯\_(ツ)_/¯
repl:
	clojure -A:dev -A:test -A:repl
