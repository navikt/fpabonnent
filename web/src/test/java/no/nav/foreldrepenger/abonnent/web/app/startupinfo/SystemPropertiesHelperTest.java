package no.nav.foreldrepenger.abonnent.web.app.startupinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.SortedMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemPropertiesHelperTest {

    private SystemPropertiesHelper helper; // objektet som testes

    @BeforeEach
    public void setup() {
        helper = SystemPropertiesHelper.getInstance();
    }

    @Test
    public void test_sysProps() {
        SortedMap<String, String> sysProps = helper.filteredSortedProperties();

        assertThat(sysProps).isNotNull();
        assertThat(sysProps.get("java.version")).isNotNull();
    }

    @Test
    public void test_envVars() {
        SortedMap<String, String> envVars = helper.filteredSortedEnvVars();

        assertThat(envVars).isNotNull();
        assertThat(envVars.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_filtrere_bort_passord_fra_java_opts() {
        var input = new HashMap<String, String>() {{
            put("JAVA_OPTS", "-Djavax.net.ssl.trustStore=/foo/bar -Djavax.net.ssl.trustStorePassword=passord_i_klartekst  -javaagent:/foo/bar/javaagent.jar  -DapplicationName=dummy -");
        }};

        SystemPropertiesHelper.filter(input);

        Assertions.assertThat(input.get("JAVA_OPTS")).isEqualTo("-Djavax.net.ssl.trustStore=/foo/bar -Djavax.net.ssl.trustStorePassword=*****  -javaagent:/foo/bar/javaagent.jar  -DapplicationName=dummy -");
    }
}
