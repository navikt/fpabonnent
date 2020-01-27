FROM navikt/java:11-appdynamics

RUN mkdir /app/lib
RUN mkdir /app/conf

# Config
COPY web/target/classes/logback.xml /app/conf/
COPY web/target/classes/jetty/jaspi-conf.xml /app/conf/

# Application Container (Jetty)
COPY web/target/app.jar /app/
COPY web/target/lib/*.jar /app/lib/
COPY 03-export-vault-secrets.sh /init-scripts/
RUN chmod +x /init-scripts/*


# Application Start Command
COPY run-java.sh /
RUN chmod +x /run-java.sh

# Export vault properties
COPY export-vault.sh /init-scripts/export-vault.sh
