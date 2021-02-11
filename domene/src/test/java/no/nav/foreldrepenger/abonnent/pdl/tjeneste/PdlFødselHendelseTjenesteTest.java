package no.nav.foreldrepenger.abonnent.pdl.tjeneste;

import static java.util.Set.of;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.AKTØR_ID_BARN;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.HENDELSE_ID;
import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.MELDINGSTYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.JsonMapper;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlFødsel;
import no.nav.foreldrepenger.abonnent.pdl.domene.internt.PdlFødselHendelsePayload;
import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.vedtak.exception.TekniskException;


public class PdlFødselHendelseTjenesteTest {

    private HendelseTjeneste hendelseTjeneste;

    @BeforeEach
    public void setUp() {
        hendelseTjeneste = new PdlFødselHendelseTjeneste();
    }

    @Test
    public void skal_mappe_fra_payload_json_til_PdlFødselHendelsePayload() {
        // Arrange
        PdlFødsel fødselmelding = HendelseTestDataUtil.lagFødselsmelding();

        // Act
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraJsonString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getHendelseType()).isEqualTo(MELDINGSTYPE.getKode());
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
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraJsonString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getHendelseType()).isEqualTo(MELDINGSTYPE.getKode());
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
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraJsonString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getHendelseType()).isEqualTo(MELDINGSTYPE.getKode());
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
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraJsonString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getHendelseType()).isEqualTo(MELDINGSTYPE.getKode());
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
        PdlFødselHendelsePayload payload = (PdlFødselHendelsePayload) hendelseTjeneste.payloadFraJsonString(JsonMapper.toJson(fødselmelding));

        // Assert
        assertThat(payload).isNotNull();
        assertThat(payload.getHendelseType()).isEqualTo(MELDINGSTYPE.getKode());
        assertThat(payload.getHendelseId()).isEqualTo(HENDELSE_ID);
        assertThat(payload.getAktørIdBarn().get()).isEmpty();
        assertThat(payload.getAktørIdForeldre()).isEmpty();
        assertThat(payload.getFødselsdato()).isEmpty();
    }

    @Test
    public void skal_få_IO_exception_ved_konvertering_av_payload_med_syntaksfeil_i_payload() {

        assertThrows(TekniskException.class, () -> hendelseTjeneste.payloadFraJsonString("{{\"foo\":\"bar\"}"));
    }
}
