package no.nav.foreldrepenger.abonnent.pdl.domene.internt;

import no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.Endringstype;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;

import org.junit.jupiter.api.Test;

import static no.nav.foreldrepenger.abonnent.testutilities.HendelseTestDataUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PdlFødselHendelsePayloadTest {

    @Test
    public void skal_mappe_til_FødselHendelseDto() {
        // Act
        HendelseWrapperDto hendelseWrapperDto = HendelseTestDataUtil.lagFødselsHendelsePayload().mapPayloadTilDto();

        // Assert
        assertThat(hendelseWrapperDto).isNotNull();
        HendelseDto hendelseDto = hendelseWrapperDto.getHendelse();
        assertThat(hendelseDto.getId()).isEqualTo(FødselHendelseDto.HENDELSE_TYPE + "_" + HENDELSE_ID);
        assertThat(hendelseDto.getHendelsetype()).isEqualTo(FødselHendelseDto.HENDELSE_TYPE);
        assertThat(hendelseDto.getEndringstype()).isEqualTo(Endringstype.OPPRETTET);
        FødselHendelseDto fødselHendelseDto = (FødselHendelseDto) hendelseDto;
        assertThat(fødselHendelseDto.getFødselsdato()).isEqualTo(FØDSELSDATO);
        assertThat(fødselHendelseDto.getAktørIdForeldre()).containsExactlyInAnyOrder(new AktørIdDto(AKTØR_ID_FAR), new AktørIdDto(AKTØR_ID_MOR));
    }
}
