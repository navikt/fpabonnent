package no.nav.foreldrepenger.abonnent.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

public class PersonTjenesteImplTest {

    private AktørConsumerMedCache aktørConsumerMock = Mockito.mock(AktørConsumerMedCache.class);
    private PersonConsumer personServiceMock = Mockito.mock(PersonConsumer.class);

    private PersonTjeneste tjeneste;

    private static final AktørId AKTØR_ID_SØKER = AktørId.dummy();
    private static final AktørId AKTØR_ID_BARN = AktørId.dummy();

    private static final LocalDate FØDSELSDATO = LocalDate.now().minusDays(2);
    private static final PersonIdent FNR_SØKER = new PersonIdent(DateTimeFormatter.ofPattern("ddMM").format(FØDSELSDATO)+"8011111");
    private static final PersonIdent FNR_BARN = new PersonIdent(DateTimeFormatter.ofPattern("ddMMyy").format(FØDSELSDATO)+"11111");

    @Before
    public void setup() {
        when(aktørConsumerMock.hentAktørIdForPersonIdent(FNR_BARN.getIdent())).thenReturn(Optional.of(AKTØR_ID_BARN.getId()));
        when(aktørConsumerMock.hentAktørIdForPersonIdent(FNR_SØKER.getIdent())).thenReturn(Optional.of(AKTØR_ID_SØKER.getId()));
        when(aktørConsumerMock.hentPersonIdentForAktørId(AKTØR_ID_BARN.getId())).thenReturn(Optional.of(FNR_BARN.getIdent()));
        when(aktørConsumerMock.hentPersonIdentForAktørId(AKTØR_ID_SØKER.getId())).thenReturn(Optional.of(FNR_SØKER.getIdent()));

        tjeneste = new PersonTjenesteImpl(aktørConsumerMock, personServiceMock);
    }

    @Test
    public void skal_svare_tomt_før_barn_registrert() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenThrow(new HentPersonPersonIkkeFunnet("bla vla", new PersonIkkeFunnet()));

        Set<AktørId> svar = tjeneste.registrerteForeldre(AKTØR_ID_BARN);

        assertThat(svar).isEmpty();
    }

    @Test
    public void skal_svare_med_mor_når_barn_registrert() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenReturn(responseMedMora(FNR_BARN.getIdent(), FNR_SØKER.getIdent()));

        Set<AktørId> svar = tjeneste.registrerteForeldre(AKTØR_ID_BARN);

        assertThat(svar).containsExactly(AKTØR_ID_SØKER);
    }

    @Test
    public void skal_svare_nei_uten_registrering() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenThrow(new HentPersonPersonIkkeFunnet("bla vla", new PersonIkkeFunnet()));

        boolean svar = tjeneste.erRegistrert(AKTØR_ID_BARN);

        assertThat(svar).isFalse();
    }

    @Test
    public void skal_svare_ja_med_registrering() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenReturn(responseMedMora(FNR_BARN.getIdent(), FNR_SØKER.getIdent()));

        boolean svar = tjeneste.erRegistrert(AKTØR_ID_BARN);

        assertThat(svar).isTrue();
    }

    @Test
    public void skal_svare_nei_uten_dødsdato() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenReturn(responseUtenDødsdato(FNR_SØKER.getIdent()));

        boolean svar = tjeneste.harRegistrertDødsdato(AKTØR_ID_SØKER);

        assertThat(svar).isFalse();
    }

    @Test
    public void skal_svare_ja_med_dødsdato() throws Exception {
        when(personServiceMock.hentPersonResponse(any())).thenReturn(responseMedDødsdato(FNR_SØKER.getIdent(), LocalDate.now().minusDays(1)));

        boolean svar = tjeneste.harRegistrertDødsdato(AKTØR_ID_SØKER);

        assertThat(svar).isTrue();
    }

    @Test
    public void skal_svare_nei_og_tomt_når_aktør_register_gir_tomt_svar() {
        when(aktørConsumerMock.hentPersonIdentForAktørId(anyString())).thenReturn(Optional.empty());

        Set<AktørId> svar1 = tjeneste.registrerteForeldre(AKTØR_ID_BARN);
        boolean svar2 = tjeneste.erRegistrert(AKTØR_ID_BARN);
        boolean svar3 = tjeneste.harRegistrertDødsdato(AKTØR_ID_SØKER);

        assertThat(svar1).isEmpty();
        assertThat(svar2).isFalse();
        assertThat(svar3).isFalse();
    }

    private HentPersonResponse responseMedDødsdato(String ident, LocalDate dødsdato) {
        return new HentPersonResponse()
            .withPerson(new Person()
                .withAktoer(new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent().withIdent(new NorskIdent().withIdent(ident)))
            .withDoedsdato(new Doedsdato().withDoedsdato(DateUtil.convertToXMLGregorianCalendar(dødsdato))));
    }

    private HentPersonResponse responseUtenDødsdato(String ident) {
        return new HentPersonResponse()
            .withPerson(new Person()
                .withAktoer(new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent().withIdent(new NorskIdent().withIdent(ident))));
    }

    private HentPersonResponse responseMedMora(String ident, String identMor) {
        return new HentPersonResponse()
            .withPerson(new Person()
                .withAktoer(new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent().withIdent(new NorskIdent().withIdent(ident)))
            .withHarFraRolleI(new Familierelasjon().withTilRolle(new Familierelasjoner().withValue("MORA"))
                .withTilPerson(new Person().withAktoer(new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent().withIdent(new NorskIdent().withIdent(identMor)))))
            );
    }
}
