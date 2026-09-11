.PHONY: clean jar ui tag image buildx run outdated install deploy tree test repl

## when VERSION is set, also push / move "latest" to it
## BUILDER: buildx builder used for multi architecture images
VERSION ?= latest
LATEST ?= true
BUILDER ?= baymax-builder

clean:
	rm -rf target
	rm -rf .env

ui:
	cd src/ui && npm install && npm run build

jar: ui
	rm -rf target && mkdir target
	clojure -M:tag
	clojure -X:uberjar :jar target/baymax-standalone.jar :main-class baymax.app

outdated:
	clojure -M:outdated

## git / maven intel about this build => target/about/META-INF/tolitius/baymax/about.edn
tag:
	clojure -M:tag

##                make image
## VERSION=0.0.42 make image
image: jar
	docker build -t baymax:$(VERSION) .

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
	$$docker_run baymax:$(VERSION)
	@echo "baymax container is up and ready to rock & roll"


## a "docker" driver builder can't bake multiple architectures into one image,
## hence a "docker-container" one: created once, reused after
make-builder = docker buildx inspect $(BUILDER) >/dev/null 2>&1 || \
               docker buildx create --name $(BUILDER) --driver docker-container --bootstrap >/dev/null

buildx:
	@$(make-builder)

##                                  make push
## VERSION=0.0.42                   make push
## VERSION=0.0.42 ARCH=multi        make push
## VERSION=0.0.42 LATEST=false      make push
push: image
	@docker login
	@latest_tag=""; \
	if [ "$(LATEST)" = "true" ] && [ "$(VERSION)" != "latest" ]; then \
		latest_tag="tolitius/baymax:latest"; \
	fi; \
	if [ "$(ARCH)" = "multi" ]; then \
		$(make-builder); \
		echo "building and pushing multi-architecture image for linux/arm64/v8, linux/amd64"; \
		docker buildx build \
			--builder $(BUILDER) \
			--platform linux/arm64/v8,linux/amd64 \
			-t tolitius/baymax:$(VERSION) \
			$${latest_tag:+-t $$latest_tag} \
			--push \
			.; \
	else \
		echo "pushing native architecture image ($$(docker version -f '{{.Client.Arch}}'))"; \
		docker tag baymax:$(VERSION) tolitius/baymax:$(VERSION); \
		docker push tolitius/baymax:$(VERSION); \
		if [ -n "$$latest_tag" ]; then \
			docker tag baymax:$(VERSION) $$latest_tag; \
			docker push $$latest_tag; \
		fi; \
	fi
	@echo "baymax image pushed to docker hub: tolitius/baymax:$(VERSION)"

install: jar
	clojure -M:install

deploy: jar
	clojure -M:deploy

tree:
	mvn dependency:tree

test:
	clojure -X:test :patterns '[".*test.*"]'

repl:
	# clojure -A:dev -A:test -A:repl
	clojure -M:dev:test:repl

