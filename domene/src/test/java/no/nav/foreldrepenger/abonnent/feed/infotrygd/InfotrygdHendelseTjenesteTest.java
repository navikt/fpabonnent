package no.nav.foreldrepenger.abonnent.feed.infotrygd;

import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_AKTØR_ID;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_FOM;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_IDENT_DATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_TYPE_YTELSE;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.KOBLING_ID;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.SEKVENSNUMMER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.abonnent.feed.domain.InfotrygdHendelsePayload;
import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.abonnent.felles.HendelseTjeneste;
import no.nav.foreldrepenger.abonnent.felles.JsonMapper;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.FeedElement;
import no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype;
import no.nav.vedtak.exception.TekniskException;

public class InfotrygdHendelseTjenesteTest {
    private static final String INFOTRYGDMELDING = no.nav.foreldrepenger.kontrakter.feed.infotrygd.v1.Meldingstype.INFOTRYGD_ENDRET.getType();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private HendelseTjeneste hendelseTjeneste;

    @Before
    public void setUp() {
        hendelseTjeneste = new InfotrygdHendelseTjeneste();
    }

    @Test
    public void skal_mappe_fra_payload_json_til_InfotrygdHendelsePayload() {
        FeedElement infotrygdMelding = HendelseTestDataUtil.lagInfotrygdMelding();

        InfotrygdHendelsePayload payload = (InfotrygdHendelsePayload) hendelseTjeneste.payloadFraString(JsonMapper.toJson(infotrygdMelding));

        assertThat(payload).isNotNull();
        assertThat(payload.getSekvensnummer()).isEqualTo(SEKVENSNUMMER);
        assertThat(payload.getKoblingId()).isEqualTo(KOBLING_ID);
        assertThat(payload.getType()).isEqualTo(INFOTRYGDMELDING);
        assertThat(payload.getAktoerId()).isEqualTo(INFOTRYGD_AKTØR_ID);
        assertThat(payload.getFom()).isEqualTo(INFOTRYGD_FOM);
        assertThat(payload.getIdentDato()).isEqualTo(INFOTRYGD_IDENT_DATO);
        assertThat(payload.getTypeYtelse()).isEqualTo(INFOTRYGD_TYPE_YTELSE);
    }

    @Test
    public void skal_kaste_teknisk_feil_hvis_innhold_er_blankt() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Kan ikke konvertere feed-content. type=" + Meldingstype.INFOTRYGD_ENDRET.getType()
                + ", sekvensnummer=" + SEKVENSNUMMER);
        FeedElement infotrygdMelding = new FeedElement.Builder().medType(INFOTRYGDMELDING).medSekvensId(SEKVENSNUMMER)
                .medInnhold("").build();

        hendelseTjeneste.payloadFraString(JsonMapper.toJson(infotrygdMelding));
    }

    @Test
    public void skal_få_IO_exception_ved_konvertering_av_payload_med_syntaksfeil_til_feed_element() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Fikk IO exception ved parsing av JSON");
        hendelseTjeneste.payloadFraString("{{\"foo\":\"bar\"}");
    }

    @Test
    public void skal_få_json_mapping_feil_ved_konvertering_av_payload_som_ikke_har_relevante_felter_til_feed_element() {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-730005:Kan ikke konvertere feed-content. type=null, sekvensnummer=0");
        hendelseTjeneste.payloadFraString("{\"foo\":\"bar\"}");
    }
}
