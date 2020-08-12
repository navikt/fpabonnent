package no.nav.foreldrepenger.abonnent.feed.tps;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_BARN;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.HENDELSE_ID;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.MELDINGSTYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.abonnent.feed.domain.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.PdlFødsel;
import no.nav.vedtak.exception.TekniskException;


public class PdlFødselHendelseTjenesteTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseTjeneste hendelseTjeneste;

    @Before
    public void setUp() {
        hendelseTjeneste = new PdlFødselHendelseTjeneste();
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload() {
        // Arrange
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding();

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn().get()).contains(AKTØR_ID_BARN);
        assertThat(payload.getAktørIdForeldre()).contains(Set.of(AKTØR_ID_MOR, AKTØR_ID_FAR));
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload_uten_ugyldige_aktørId_på_barn() {
        // Arrange
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding(of("26364656768", "234567"),
                of(AKTØR_ID_MOR, AKTØR_ID_FAR), FØDSELSDATO);

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn().get()).isEmpty();
        assertThat(payload.getAktørIdForeldre()).contains(Set.of(AKTØR_ID_MOR, AKTØR_ID_FAR));
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload_flere_identer_matcher_aktørId() {
        // Arrange
        Set<String> aktørIdBarn = of("1234567890986", "1234567890987");
        Set<String> aktørIdForeldre = of("1234567890989", "1234567890988", "1234567890990", "1234567890991");
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding(aktørIdBarn, aktørIdForeldre, FØDSELSDATO);

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdBarn().get()).hasSize(2).hasSameElementsAs(aktørIdBarn);
        assertThat(payload.getAktørIdForeldre()).isPresent();
        assertThat(payload.getAktørIdForeldre().get()).hasSize(4).hasSameElementsAs(aktørIdForeldre);
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload_flere_identer_er_gyldig() {
        // Arrange
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding(of("26364656768", "1234567890987"),
                of("10018876555", "1234567890988", "30102040506", "1234567890989"), FØDSELSDATO);

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn()).isPresent();
        assertThat(payload.getAktørIdForeldre()).isPresent();
        assertThat(payload.getFødselsdato()).hasValue(FØDSELSDATO);
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload_med_tomme_identer() {
        // Arrange
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding(Collections.emptySet(), Collections.emptySet(), null);

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn().get()).isEmpty();
        assertThat(payload.getAktørIdForeldre()).isEmpty();
        assertThat(payload.getFødselsdato()).isEmpty();
    }

    @Test
    public void skal_få_IO_exception_ved_konvertering_av_payload_med_syntaksfeil_i_payload() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Fikk IO exception ved parsing av JSON");
        hendelseTjeneste.payloadFraString("{{\"foo\":\"bar\"}");
    }
}
