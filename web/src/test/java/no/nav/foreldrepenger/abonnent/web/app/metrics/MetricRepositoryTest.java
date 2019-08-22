package no.nav.foreldrepenger.abonnent.web.app.metrics;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.vedtak.felles.jpa.OracleVersionChecker;

public class MetricRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private OracleVersionChecker versionChecker = new OracleVersionChecker(repositoryRule.getEntityManager());
    private MetricRepository repository = new MetricRepository(repositoryRule.getEntityManager(), versionChecker);

    @Test
    public void skal_hente_antall_prosess_tasks_per_type() throws Exception {
        final List<Object[]> list = repository.tellAntallProsessTaskerPerTypeOgStatus();
        assertThat(list).isNotNull();
    }

    @Test
    public void skal_hente_prosesstasks_med_prefix() throws Exception {
        final List<String> list = repository.hentProsessTaskTyperMedPrefixer(Collections.singletonList(""));
        assertThat(list).isNotNull();
    }
}
