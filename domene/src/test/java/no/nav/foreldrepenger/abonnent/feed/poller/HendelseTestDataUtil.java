package no.nav.foreldrepenger.abonnent.feed.poller;

import static java.util.Collections.singletonList;
import static java.util.Set.of;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.InfotrygdHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.domain.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.feed.tps.TpsHendelseHjelper;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.InfotrygdEndret;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.Ident;
import no.nav.tjenester.person.feed.v2.foedselsmelding.FoedselsmeldingOpprettet;

public class HendelseTestDataUtil {

    private static final HendelseType MELDINGSTYPE = HendelseType.FØDSELSMELDINGOPPRETTET;
    public static final Long SEKVENSNUMMER = 1L;
    public static final Long KOBLING_ID = 2L;
    public static final LocalDate FØDSELSDATO = LocalDate.of(2018, 1,30);
    public static final String AKTØR_ID_BARN = "1678462152535";
    public static final String AKTØR_ID_MOR = "1678462152536";
    public static final String AKTØR_ID_FAR = "1678462152537";

    public static final String INFOTRYGD_AKTØR_ID = "1000045443911";
    public static final LocalDate INFOTRYGD_FOM = LocalDate.of(2018, 1, 11);
    public static final String INFOTRYGD_IDENT_DATO = "20181101";
    public static final String INFOTRYGD_TYPE_YTELSE = "BS";

    public static FeedEntry lagFødselsmelding() {
        FoedselsmeldingOpprettet fodsel = new FoedselsmeldingOpprettet();
        fodsel.setFoedselsdato(FØDSELSDATO);
        fodsel.setPersonIdenterBarn(of(lagAktørIdIdent(AKTØR_ID_BARN)));
        fodsel.setPersonIdenterMor(of(lagAktørIdIdent(AKTØR_ID_MOR)));
        fodsel.setPersonIdenterFar(of(lagAktørIdIdent(AKTØR_ID_FAR)));

        return FeedEntry.builder()
                .type(MELDINGSTYPE.getKode())
                .sequence(SEKVENSNUMMER)
                .content(fodsel)
                .build();
    }

    public static FeedElement lagInfotrygdMelding(Long sekvensnummer, Long koblingId) {
        InfotrygdEndret innhold = new InfotrygdEndret();
        innhold.setAktoerId(INFOTRYGD_AKTØR_ID);
        innhold.setFom(INFOTRYGD_FOM);
        innhold.setIdentDato(INFOTRYGD_IDENT_DATO);
        innhold.setTypeYtelse(INFOTRYGD_TYPE_YTELSE);
        return new FeedElement.Builder()
                .medSekvensId(sekvensnummer)
                .medKoblingId(koblingId)
                .medType(no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype.INFOTRYGD_ENDRET.getType())
                .medInnhold(innhold)
                .build();
    }

    public static FeedElement lagInfotrygdMelding() {
        return lagInfotrygdMelding(SEKVENSNUMMER, KOBLING_ID);
    }

    public static FeedEntry lagFødselsmelding(Set<Ident> aktørIdBarn, Set<Ident> aktørIdMor, Set<Ident> aktørIdFar, LocalDate fødselsDato) {
        FoedselsmeldingOpprettet fodsel = new FoedselsmeldingOpprettet();
        fodsel.setFoedselsdato(fødselsDato);
        fodsel.setPersonIdenterBarn(aktørIdBarn);
        fodsel.setPersonIdenterMor(aktørIdMor);
        fodsel.setPersonIdenterFar(aktørIdFar);

        return FeedEntry.builder()
                .type(MELDINGSTYPE.getKode())
                .sequence(SEKVENSNUMMER)
                .content(fodsel)
                .build();
    }

    public static FødselHendelsePayload lagFødselsHendelsePayload() {
        FødselHendelsePayload.Builder builder = new FødselHendelsePayload.Builder();
        return builder
                .sekvensnummer(SEKVENSNUMMER)
                .type(MELDINGSTYPE.getKode())
                .aktørIdBarn(new HashSet<>(singletonList(AKTØR_ID_BARN)))
                .aktørIdMor(new HashSet<>(singletonList(AKTØR_ID_MOR)))
                .aktørIdFar(new HashSet<>(singletonList(AKTØR_ID_FAR)))
                .fødselsdato(FØDSELSDATO)
                .build();
    }

    public static InfotrygdHendelsePayload lagInfotrygdHendelsePayload() {
        return new InfotrygdHendelsePayload.Builder()
                .sekvensnummer(SEKVENSNUMMER)
                .koblingId(KOBLING_ID)
                .type(no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype.INFOTRYGD_INNVILGET.getType())
                .aktørId(INFOTRYGD_AKTØR_ID)
                .fom(INFOTRYGD_FOM)
                .identDato(INFOTRYGD_IDENT_DATO)
                .typeYtelse(INFOTRYGD_TYPE_YTELSE)
                .build();
    }

    public static InngåendeHendelse lagInngåendeFødselsHendelse(long id, String uuid, HåndtertStatusType håndtertStatus) {
        return InngåendeHendelse.builder()
                .sekvensnummer(id)
                .type(MELDINGSTYPE)
                .payload("payload")
                .feedKode(FeedKode.TPS)
                .requestUuid(uuid)
                .håndtertStatus(håndtertStatus)
                .build();
    }

    public static Ident lagFnrIdent(String fnr) {
        if (Objects.isNull(fnr)) {
            return null;
        }
        return new Ident(fnr, TpsHendelseHjelper.FNR_IDENT_TYPE);
    }

    public static Ident lagAktørIdIdent(String aktørId) {
        if (Objects.isNull(aktørId)) {
            return null;
        }
        return new Ident(aktørId, TpsHendelseHjelper.AKTØR_ID_IDENT_TYPE);
    }
}
