package no.nav.foreldrepenger.abonnent.feed.poller;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.domain.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlEndringstype;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;

public class HendelseTestDataUtil {

    public static final HendelseType MELDINGSTYPE = HendelseType.PDL_FØDSEL_OPPRETTET;
    public static final String HENDELSE_ID = UUID.randomUUID().toString();
    public static final LocalDate FØDSELSDATO = LocalDate.of(2018, 1,30);
    public static final String AKTØR_ID_BARN = "1678462152535";
    public static final String AKTØR_ID_MOR = "1678462152536";
    public static final String AKTØR_ID_FAR = "1678462152537";

    public static PdlFødsel lagFødselsmelding() {
        PdlFødsel.Builder fødsel = new PdlFødsel.Builder();
        fødsel.medHendelseId(HENDELSE_ID);
        fødsel.medHendelseType(MELDINGSTYPE);
        fødsel.medEndringstype(PdlEndringstype.OPPRETTET);
        fødsel.medFødselsdato(FØDSELSDATO);
        fødsel.leggTilPersonident(AKTØR_ID_BARN);

        PdlFødsel pdlFødsel = fødsel.build();
        pdlFødsel.setAktørIdForeldre(Set.of(AKTØR_ID_MOR, AKTØR_ID_FAR));

        return pdlFødsel;
    }

    public static PdlFødsel lagFødselsmelding(Set<String> aktørIdBarn, Set<String> aktørIdForeldre, LocalDate fødselsdato) {
        return lagFødselsmelding(HENDELSE_ID, aktørIdBarn, aktørIdForeldre, fødselsdato);
    }

    public static PdlFødsel lagFødselsmelding(String hendelseId, Set<String> aktørIdBarn, Set<String> aktørIdForeldre, LocalDate fødselsdato) {
        PdlFødsel.Builder fødsel = new PdlFødsel.Builder();
        fødsel.medHendelseId(hendelseId);
        fødsel.medHendelseType(MELDINGSTYPE);
        fødsel.medEndringstype(PdlEndringstype.OPPRETTET);
        fødsel.medFødselsdato(fødselsdato);
        aktørIdBarn.stream().forEach(fødsel::leggTilPersonident);

        PdlFødsel pdlFødsel = fødsel.build();
        pdlFødsel.setAktørIdForeldre(aktørIdForeldre);

        return pdlFødsel;
    }

    public static PdlFødselHendelsePayload lagFødselsHendelsePayload() {
        PdlFødselHendelsePayload.Builder builder = new PdlFødselHendelsePayload.Builder();
        return builder
                .hendelseId(HENDELSE_ID)
                .type(MELDINGSTYPE.getKode())
                .endringstype("OPPRETTET")
                .aktørIdBarn(new HashSet<>(singletonList(AKTØR_ID_BARN)))
                .aktørIdForeldre(Set.of(AKTØR_ID_MOR, AKTØR_ID_FAR))
                .fødselsdato(FØDSELSDATO)
                .build();
    }

    public static InngåendeHendelse lagInngåendeFødselsHendelse(String hendelseId, String requestUuid, HåndtertStatusType håndtertStatus) {
        return InngåendeHendelse.builder()
                .hendelseId(hendelseId)
                .type(MELDINGSTYPE)
                .payload("payload")
                .feedKode(FeedKode.PDL)
                .requestUuid(requestUuid)
                .håndtertStatus(håndtertStatus)
                .build();
    }
}
