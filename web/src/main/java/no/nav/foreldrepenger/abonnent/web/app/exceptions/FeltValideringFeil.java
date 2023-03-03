package no.nav.foreldrepenger.abonnent.web.app.exceptions;

import java.util.List;

import no.nav.vedtak.exception.FunksjonellException;

public class FeltValideringFeil {

    static FunksjonellException feltverdiKanIkkeValideres(List<String> feltnavn) {
        return new FunksjonellException("FPT-328673",
            String.format("Det oppstod en valideringsfeil på felt %s. Vennligst kontroller at alle feltverdier er korrekte.", feltnavn),
            "Kontroller at alle feltverdier er korrekte");
    }
}
