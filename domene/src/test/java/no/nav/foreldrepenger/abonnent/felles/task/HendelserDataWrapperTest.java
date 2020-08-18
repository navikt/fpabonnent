package no.nav.foreldrepenger.abonnent.felles.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class HendelserDataWrapperTest {

    private static final String PROSESSTASK_STEG1 = "prosesstask.steg1";
    private static final String PROSESSTASK_STEG2 = "prosesstask.steg2";

    private ProsessTaskData eksisterendeData;
    private HendelserDataWrapper wrapper;

    @Before
    public void setup() {
        eksisterendeData = new ProsessTaskData(PROSESSTASK_STEG1);
        eksisterendeData.setSekvens("1");
        wrapper = new HendelserDataWrapper(eksisterendeData);
    }

    @Test
    public void test_kan_opprette_og_kopiere_wrapper_uten_eksisterende_properties() throws Exception {
        wrapper = new HendelserDataWrapper(eksisterendeData);
        assertThat(wrapper.hentAlleProsessTaskVerdier()).as("Forventer at wrapper i utgangspunktet blir opprettet uten properties").isEmpty();

        HendelserDataWrapper wrapperNesteSteg = wrapper.nesteSteg(PROSESSTASK_STEG2);
        assertThat(wrapper.hentAlleProsessTaskVerdier()).as("").isEqualTo(wrapperNesteSteg.hentAlleProsessTaskVerdier());
    }

    @Test
    public void test_beholder_properties_og_payload_fra_forrige_steg() throws Exception {
        String innPayload = "<xml>test</xml>";
        wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseId("72ee35aa-002f-400e-9956-8d8674aea949");
        wrapper.setPayload(innPayload);

        HendelserDataWrapper wrapperNesteSteg = wrapper.nesteSteg(PROSESSTASK_STEG2);
        assertThat(wrapperNesteSteg.getHendelseId()).as("Forventer at hendelseId blir med til neste steg.").isEqualTo(wrapper.getHendelseId());
        assertThat(wrapperNesteSteg.getProsessTaskData().getPayload()).as("Forventer at payload også blir kopiert over").isNotNull();
        assertThat(wrapperNesteSteg.getPayloadAsString()).as("Forventer at payload kan hentes ut som string").hasValue(innPayload);
    }

    @Test
    public void skal_hente_hendelse_sekvensnummer_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseId("1");
        assertThat(wrapper.getHendelseId()).hasValue("1");
    }

    @Test
    public void skal_hente_hendelsetype_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseType("HENDELSE_TYPE");
        assertThat(wrapper.getHendelseType()).hasValue("HENDELSE_TYPE");
    }

    @Test
    public void skal_hente_aktøridForeldre_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setAktørIdForeldre(Set.of("1234567891238", "1234567891235"));
        assertThat(wrapper.getAktørIdForeldre().get()).contains("1234567891238", "1234567891235");
    }

    @Test
    public void skal_hente_aktøridBarn_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setAktørIdBarn(Set.of("2131234567891"));
        assertThat(wrapper.getAktørIdBarn().get()).contains("2131234567891");
    }

    @Test
    public void skal_hente_fødselsdato_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setFødselsdato(LocalDate.of(2018, 1,10));
        assertThat(wrapper.getFødselsdato()).hasValue("2018-01-10");
    }

}
