.PHONY: clean jar tag outdated install deploy tree test repl

clean:
	rm -rf target

jar: tag
	clojure -X:uberjar :jar baymax.jar :main-class baymax.app

outdated:
	clojure -M:outdated

tag:
	clojure -A:tag

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
