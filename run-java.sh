#!/usr/bin/env sh
set -eu

hostname=$(hostname)

export JAVA_OPTS="${JAVA_OPTS:-} -Xmx512m -Xms128m -Djava.security.egd=file:/dev/./urandom"

if test -r "${NAV_TRUSTSTORE_PATH:-}";
then
    if ! echo "${NAV_TRUSTSTORE_PASSWORD}" | keytool -list -keystore ${NAV_TRUSTSTORE_PATH} > /dev/null;
    then
        echo Truststore is corrupt, or bad password
        exit 1
    fi

    JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=${NAV_TRUSTSTORE_PATH}"
    JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStorePassword=${NAV_TRUSTSTORE_PASSWORD}"
fi

# hvor skal gc log, heap dump etc kunne skrives til med Docker?
export todo_JAVA_OPTS="${JAVA_OPTS} -XX:ErrorFile=./hs_err_pid<pid>.log -XX:HeapDumpPath=./java_pid<pid>.hprof -XX:-HeapDumpOnOutOfMemoryError -Xloggc:<filename>"

export STARTUP_CLASS=${STARTUP_CLASS:-"no.nav.foreldrepenger.abonnent.web.server.JettyServer"}
export LOGBACK_CONFIG=${LOGBACK_CONFIG:-"./conf/logback.xml"}
export CLASSPATH="app.jar:lib/*"

exec java -cp ${CLASSPATH?} ${DEFAULT_JAVA_OPTS:-} ${JAVA_OPTS} -Dlogback.configurationFile=${LOGBACK_CONFIG?} -Dwebapp=${WEBAPP:-"./webapp"} -Dapplication.name=FPABONNENT -Dfeed.pagesize.value=${FEED_PAGESIZE:-"1000"} ${STARTUP_CLASS?} ${SERVER_PORT:-8080} $@
