package no.nav.foreldrepenger.abonnent.web.app.konfig;

import java.util.Collection;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.abonnent.web.app.jackson.JacksonJsonConfig;
import no.nav.vedtak.felles.integrasjon.rest.jersey.TimingFilter;

public class FellesKlasserForRest {

    private FellesKlasserForRest() {

    }

    public static Collection<Class<?>> getClasses() {
        return Set.of(JacksonJsonConfig.class,
                GeneralRestExceptionMapper.class,
                TimingFilter.class);
    }
}
