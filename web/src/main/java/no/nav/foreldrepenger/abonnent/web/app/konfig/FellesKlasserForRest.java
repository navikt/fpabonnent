package no.nav.foreldrepenger.abonnent.web.app.konfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.abonnent.web.app.jackson.JacksonJsonConfig;

public class FellesKlasserForRest {

    private static final Set<Class<?>> CLASSES;

    static {
        Set<Class<?>> klasser = new HashSet<>();
        klasser.add(JacksonJsonConfig.class);
        klasser.add(GeneralRestExceptionMapper.class);
        CLASSES = Collections.unmodifiableSet(klasser);
    }

    private FellesKlasserForRest() {

    }

    public static Collection<Class<?>> getClasses() {
        return CLASSES;
    }
}
