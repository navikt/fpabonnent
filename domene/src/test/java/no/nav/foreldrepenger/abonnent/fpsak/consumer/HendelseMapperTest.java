package no.nav.foreldrepenger.abonnent.fpsak.consumer;

import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_FAR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.AKTØR_ID_MOR;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil.SEKVENSNUMMER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.abonnent.feed.poller.HendelseTestDataUtil;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;

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
        assertThat(fødselHendelseDto.getAktørIdForeldre()).containsExactly(new AktørIdDto(AKTØR_ID_FAR), new AktørIdDto(AKTØR_ID_MOR));
    }

}
