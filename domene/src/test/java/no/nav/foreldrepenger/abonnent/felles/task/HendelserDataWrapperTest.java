package no.nav.foreldrepenger.abonnent.felles.task;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HendelserDataWrapperTest {

    private static final TaskType PROSESSTASK_STEG1 = new TaskType("prosesstask.steg1");
    private static final TaskType PROSESSTASK_STEG2 = new TaskType("prosesstask.steg2");

    private ProsessTaskData eksisterendeData;
    private HendelserDataWrapper wrapper;

    @BeforeEach
    void setup() {
        eksisterendeData = ProsessTaskData.forTaskType(PROSESSTASK_STEG1);
        wrapper = new HendelserDataWrapper(eksisterendeData);
    }

    @Test
    void test_kan_opprette_og_kopiere_wrapper_uten_eksisterende_properties() {
        wrapper = new HendelserDataWrapper(eksisterendeData);
        assertThat(wrapper.hentAlleProsessTaskVerdier()).as("Forventer at wrapper i utgangspunktet blir opprettet uten properties").isEmpty();

        HendelserDataWrapper wrapperNesteSteg = wrapper.nesteSteg(PROSESSTASK_STEG2);
        assertThat(wrapper.hentAlleProsessTaskVerdier()).as("").isEqualTo(wrapperNesteSteg.hentAlleProsessTaskVerdier());
    }

    @Test
    void test_beholder_properties_og_payload_fra_forrige_steg() {
        String innPayload = "<xml>test</xml>";
        wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseId("72ee35aa-002f-400e-9956-8d8674aea949");
        eksisterendeData.setPayload(innPayload);

        HendelserDataWrapper wrapperNesteSteg = wrapper.nesteSteg(PROSESSTASK_STEG2);
        assertThat(wrapperNesteSteg.getHendelseId()).as("Forventer at hendelseId blir med til neste steg.").isEqualTo(wrapper.getHendelseId());
        assertThat(wrapperNesteSteg.getProsessTaskData().getPayloadAsString()).as("Forventer at payload også blir kopiert over").isNotNull();
    }

    @Test
    void skal_hente_hendelse_id_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseId("1");
        assertThat(wrapper.getHendelseId()).hasValue("1");
    }

    @Test
    void skal_hente_hendelse_type_fra_properties() {
        HendelserDataWrapper wrapper = new HendelserDataWrapper(eksisterendeData);
        wrapper.setHendelseType("HENDELSE_TYPE");
        assertThat(wrapper.getHendelseType()).hasValue("HENDELSE_TYPE");
    }
}
