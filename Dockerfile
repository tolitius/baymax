FROM alpine:3.19

LABEL maintainer="https://github.com/tolitius"
LABEL description="baymax: your visibility and insights loyal companion"

RUN apk -U upgrade && \
    apk add libstdc++ curl ca-certificates bash iptables ip6tables iproute2 drill netcat-openbsd openjdk21

ENV APP_NAME=baymax

WORKDIR /opt/app/${APP_NAME}

COPY target/${APP_NAME}-standalone.jar lib/${APP_NAME}-standalone.jar
COPY resources/logback.xml conf/

CMD ["java", "-Dconf=/opt/app/baymax/config.edn", "-jar", "lib/baymax-standalone.jar"]
