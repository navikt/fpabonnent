FROM ghcr.io/navikt/fp-baseimages/java:21
LABEL org.opencontainers.image.source=https://github.com/navikt/fpabonnent

# Config
COPY web/target/classes/logback*.xml ./conf/

# Application Container (Jetty)
COPY web/target/lib/*.jar ./lib/
COPY web/target/app.jar .
