.PHONY: clean jar ui tag image run outdated install deploy tree test repl

clean:
	rm -rf target
	rm -rf .env

ui:
	cd src/ui && npm install && npm run build

jar: tag ui
	rm -rf target && mkdir target
	clojure -X:uberjar :jar target/baymax-standalone.jar :main-class baymax.app

outdated:
	clojure -M:outdated

tag:
	clojure -A:tag

##                make image
## VERSION=0.0.42 make image
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


## ARCH=multi       make push
## ARCH=linux/amd64 make push
push:
	@docker login
	@if [ -n "$(ARCH)" ] && [ "$(ARCH)" = "multi" ]; then \
		echo "building and pushing multi-architecture image for linux/arm64/v8, linux/amd64"; \
		docker buildx build \
			--platform linux/arm64/v8,linux/amd64 \
			-t tolitius/baymax:$${VERSION:-latest} \
			--push \
			.; \
	else \
		echo "pushing native architecture image ($$(docker version -f '{{.Client.Arch}}'))"; \
		docker tag baymax:$${VERSION:-latest} tolitius/baymax:$${VERSION:-latest}; \
		docker push tolitius/baymax:$${VERSION:-latest}; \
	fi
	@echo "baymax image pushed to docker hub: tolitius/baymax:$${VERSION:-latest}"

install: jar
	clojure -A:install

deploy: jar
	clojure -A:deploy

tree:
	mvn dependency:tree

test:
	clojure -X:test :patterns '[".*test.*"]'

repl:
	# clojure -A:dev -A:test -A:repl
	clojure -M:dev:test:repl

