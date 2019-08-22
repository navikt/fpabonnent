package no.nav.foreldrepenger.abonnent.fpsak.consumer;

import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_AKTØR_ID;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_FOM;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_IDENT_DATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.INFOTRYGD_TYPE_YTELSE;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.SEKVENSNUMMER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto;

public class HendelseMapperTest {

    private HendelseMapper mapper = new HendelseMapper();

    @Test
    public void skal_mappe_til_FødselHendelseDto() {
        String forventet_uid = HendelseMapper.FØDSEL_HENDELSE_TYPE + SEKVENSNUMMER;
        HendelseWrapperDto hendelseWrapperDto = mapper.map(HendelseTestDataUtil.lagFødselsHendelsePayload());

        assertThat(hendelseWrapperDto).isNotNull();

        HendelseDto hendelseDto = hendelseWrapperDto.getHendelse();

        assertThat(hendelseDto.getId()).isEqualTo(forventet_uid);
        assertThat(hendelseDto.getHendelsetype()).isEqualTo(HendelseMapper.FØDSEL_HENDELSE_TYPE);
        FødselHendelseDto fødselHendelseDto = (FødselHendelseDto) hendelseDto;
        assertThat(fødselHendelseDto.getFødselsdato()).isEqualTo(FØDSELSDATO);
        assertThat(fødselHendelseDto.getAktørIdForeldre()).containsExactly(AKTØR_ID_FAR, AKTØR_ID_MOR);
    }

    @Test
    public void skal_mappe_til_InfotrygdHendelseDto() {
        String infotrygd_uid = HendelseMapper.INFOTRYGD_HENDELSE_TYPE + SEKVENSNUMMER;
        HendelseWrapperDto hendelseWrapperDto = mapper.map(HendelseTestDataUtil.lagInfotrygdHendelsePayload());
        assertThat(hendelseWrapperDto).isNotNull();
        HendelseDto hendelseDto = hendelseWrapperDto.getHendelse();
        assertThat(hendelseDto.getId()).isEqualTo(infotrygd_uid);
        InfotrygdHendelseDto infotrygdHendelseDto = (InfotrygdHendelseDto) hendelseDto;
        assertThat(infotrygdHendelseDto.getAktørId()).contains(INFOTRYGD_AKTØR_ID);
        assertThat(infotrygdHendelseDto.getFom()).isEqualTo(INFOTRYGD_FOM);
        assertThat(infotrygdHendelseDto.getHendelsetype()).isEqualTo(new InfotrygdHendelseDto(InfotrygdHendelseDto.Hendelsetype.YTELSE_INNVILGET).getHendelsetype());
        assertThat(infotrygdHendelseDto.getIdentdato()).isEqualTo(INFOTRYGD_IDENT_DATO);
        assertThat(infotrygdHendelseDto.getTypeYtelse()).isEqualTo(INFOTRYGD_TYPE_YTELSE);
        assertThat(infotrygdHendelseDto.getAvsenderSystem()).isEqualTo(new InfotrygdHendelseDto(InfotrygdHendelseDto.Hendelsetype.YTELSE_INNVILGET).getAvsenderSystem());
    }
}
