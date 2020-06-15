package no.nav.foreldrepenger.abonnent.pdl;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.util.env.Cluster;
import no.nav.vedtak.util.env.Environment;

/**
 * Alternativ til ustabil / ikke-fungerende Unleash...
 */
@ApplicationScoped
public class PdlFeatureToggleTjeneste {

    private static final Set<Cluster> SKAL_KONSUMERE_PDL_AKTIVERT = Set.of(Cluster.LOCAL, Cluster.DEV_FSS, Cluster.PROD_FSS);
    private static final Set<Cluster> SKAL_LAGRE_PDL_AKTIVERT = Set.of(Cluster.LOCAL, Cluster.DEV_FSS, Cluster.PROD_FSS);
    private static final Set<Cluster> SKAL_GROVSORTERE_PDL_AKTIVERT = Set.of(Cluster.LOCAL, Cluster.DEV_FSS);
    private static final Set<Cluster> SKAL_SENDE_PDL_OG_DUPLIKATSJEKKE_PF_AKTIVERT = Set.of(Cluster.LOCAL, Cluster.DEV_FSS);
    private static final Set<Cluster> SKAL_KONSUMERE_PF_AKTIVERT = Set.of(Cluster.LOCAL, Cluster.DEV_FSS, Cluster.PROD_FSS);
    private static final Cluster CLUSTER = Environment.current().getCluster();

    public boolean skalKonsumerePdl() {
        return SKAL_KONSUMERE_PDL_AKTIVERT.contains(CLUSTER);
    }

    public boolean skalLagrePdl() {
        return SKAL_LAGRE_PDL_AKTIVERT.contains(CLUSTER);
    }

    public boolean skalGrovsorterePdl() {
        return SKAL_GROVSORTERE_PDL_AKTIVERT.contains(CLUSTER);
    }

    public boolean skalSendePdlOgDuplikatsjekkePf() {
        return SKAL_SENDE_PDL_OG_DUPLIKATSJEKKE_PF_AKTIVERT.contains(CLUSTER);
    }

    public boolean skalKonsumerePf() {
        return SKAL_KONSUMERE_PF_AKTIVERT.contains(CLUSTER);
    }
}
