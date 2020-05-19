package no.nav.foreldrepenger.abonnent.feed.tps;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_BARN;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.SEKVENSNUMMER;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.lagAktørIdIdent;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.lagFnrIdent;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.abonnent.feed.domain.FødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.tjenester.person.feed.common.v1.FeedEntry;
import no.nav.tjenester.person.feed.v2.Ident;
import no.nav.tjenester.person.feed.v2.Meldingstype;
import no.nav.tjenester.person.feed.v2.foedselsmelding.FoedselsmeldingOpprettet;
import no.nav.vedtak.exception.TekniskException;


public class FødselsmeldingOpprettetHendelseTjenesteTest {
    private static final String FØDSELSMELDING = Meldingstype.FOEDSELSMELDINGOPPRETTET.name();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseTjeneste hendelseTjeneste;

    @Before
    public void setUp() {
        hendelseTjeneste = new FødselsmeldingOpprettetHendelseTjeneste();
    }

    @Test
    public void skal_mappe_fra_payload_json_til_FødselHendelsePayload() {
        FeedEntry fødselmelding = HendelseTestDataUtil.lagFødselsmelding();

        FødselHendelsePayload payload = (FødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDING);
        assertThat(payload.getHendelseId()).isEqualTo("" + SEKVENSNUMMER);
        assertThat(payload.getAktørIdBarn().get()).contains(AKTØR_ID_BARN);
        assertThat(payload.getAktørIdMor().get()).contains(AKTØR_ID_MOR);
        assertThat(payload.getAktørIdFar().get()).contains(AKTØR_ID_FAR);
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_FødselHendelsePayload_ugyldig_aktørId() {
        FeedEntry fødselmelding = HendelseTestDataUtil.lagFødselsmelding(of(lagFnrIdent("26364656768"), lagFnrIdent("234567")),
                of(lagFnrIdent("10018876555")), of(lagFnrIdent("30102040506")), FØDSELSDATO);

        FødselHendelsePayload payload = (FødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDING);
        assertThat(payload.getHendelseId()).isEqualTo("" + SEKVENSNUMMER);
        assertThat(payload.getAktørIdBarn().get()).isEmpty();
        assertThat(payload.getAktørIdMor().get()).isEmpty();
        assertThat(payload.getAktørIdFar().get()).isEmpty();
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_FødselHendelsePayload_flere_identer_matcher_aktørId() {
        Set<Ident> aktørIdBarn = of(lagAktørIdIdent("1234567890986"), lagAktørIdIdent("1234567890987"));
        Set<Ident> aktørIdMor = of(lagAktørIdIdent("1234567890989"), lagAktørIdIdent("1234567890988"));
        Set<Ident> aktørIdFar = of(lagAktørIdIdent("1234567890990"), lagAktørIdIdent("1234567890991"));
        FeedEntry fødselmelding = HendelseTestDataUtil.lagFødselsmelding(aktørIdBarn, aktørIdMor, aktørIdFar, FØDSELSDATO);

        FødselHendelsePayload payload = (FødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDING);
        assertThat(payload.getHendelseId()).isEqualTo("" + SEKVENSNUMMER);
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdBarn().get()).hasSize(2).hasSameElementsAs(aktørIdBarn.stream().map(Ident::getIdent).collect(Collectors.toList()));
        assertThat(payload.getAktørIdMor()).isPresent();
        assertThat(payload.getAktørIdMor().get()).hasSize(2).hasSameElementsAs(aktørIdMor.stream().map(Ident::getIdent).collect(Collectors.toList()));
        assertThat(payload.getAktørIdFar()).isPresent();
        assertThat(payload.getAktørIdFar().get()).hasSize(2).hasSameElementsAs(aktørIdFar.stream().map(Ident::getIdent).collect(Collectors.toList()));
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_FødselHendelsePayload_flere_identer_en_gyldig() {
        FeedEntry fødselmelding = HendelseTestDataUtil.lagFødselsmelding(of(lagFnrIdent("26364656768"), lagAktørIdIdent("1234567890987")),
                of(lagFnrIdent("10018876555"), lagAktørIdIdent("1234567890988")),
                of(lagFnrIdent("30102040506"), lagAktørIdIdent("1234567890989")), FØDSELSDATO);

        FødselHendelsePayload payload = (FødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDING);
        assertThat(payload.getHendelseId()).isEqualTo("" + SEKVENSNUMMER);
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdMor()).isPresent();
        assertThat(payload.getAktørIdFar()).isPresent();
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_FødselHendelsePayload_med_tomme_identer() {
        FeedEntry fødselmelding = FeedEntry.builder().type(FØDSELSMELDING).sequence(1).content(new FoedselsmeldingOpprettet()).build();

        FødselHendelsePayload payload =  (FødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(FØDSELSMELDING);
        assertThat(payload.getHendelseId()).isEqualTo("" + SEKVENSNUMMER);
        assertThat(payload.getAktørIdBarn()).isEmpty();
        assertThat(payload.getAktørIdMor()).isEmpty();
        assertThat(payload.getAktørIdFar()).isEmpty();
        assertThat(payload.getFødselsdato()).isEmpty();
    }

    @Test
    public void skal_kaste_teknisk_feil_hvis_content_er_null() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Kan ikke konvertere feed-content. type=" + Meldingstype.FOEDSELSMELDINGOPPRETTET.name()
                + ", sekvensnummer=" + SEKVENSNUMMER);
        FeedEntry fødselmelding = FeedEntry.builder().type(FØDSELSMELDING).sequence(1).build();

        hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));
    }

    @Test
    public void skal_få_IO_exception_ved_konvertering_av_payload_med_syntaksfeil_til_feed_entry() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Fikk IO exception ved parsing av JSON");
        hendelseTjeneste.payloadFraString("{{\"foo\":\"bar\"}");
    }

    @Test
    public void skal_få_json_mapping_feil_ved_konvertering_av_payload_som_ikke_har_relevante_felter_til_feed_entry() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-730005:Kan ikke konvertere feed-content. type=null, sekvensnummer=0");
        hendelseTjeneste.payloadFraString("{\"foo\":\"bar\"}");
    }
}
