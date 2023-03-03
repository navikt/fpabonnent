package no.nav.foreldrepenger.abonnent.felles.task;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class HendelserDataWrapper {

    public static final String HENDELSE_ID = "hendelse.id";
    public static final String INNGÅENDE_HENDELSE_ID = "hendelse.ihId";
    public static final String HENDELSE_TYPE = "hendelse.type";

    private final ProsessTaskData prosessTaskData;

    public HendelserDataWrapper(ProsessTaskData prosessTaskData) {
        this.prosessTaskData = prosessTaskData;
    }

    public ProsessTaskData getProsessTaskData() {
        return prosessTaskData;
    }

    public HendelserDataWrapper nesteSteg(TaskType steg) {
        ProsessTaskData nesteStegProsessTaskData = ProsessTaskData.forTaskType(steg);

        String taskSekvensnummer = Optional.ofNullable(getProsessTaskData().getSekvens()).orElse("1");
        Long taskSekvens = Long.parseLong(taskSekvensnummer) + 1;
        nesteStegProsessTaskData.setSekvens(taskSekvens.toString());
        HendelserDataWrapper neste = new HendelserDataWrapper(nesteStegProsessTaskData);
        neste.copyData(this);
        return neste;
    }

    private void copyData(HendelserDataWrapper fra) {
        this.addProperties(fra.prosessTaskData.getProperties());
        this.setPayload(fra.prosessTaskData.getPayloadAsString());
        this.getProsessTaskData().setGruppe(fra.getProsessTaskData().getGruppe());
    }

    private void addProperties(Properties newProps) {
        prosessTaskData.getProperties().putAll(newProps);
    }

    public void setPayload(String payload) {
        prosessTaskData.setPayload(payload);
    }

    public Properties hentAlleProsessTaskVerdier() {
        return prosessTaskData.getProperties();
    }

    public Long getId() {
        return prosessTaskData.getId();
    }

    public Optional<String> getHendelseId() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_ID));
    }

    public void setHendelseId(String hendelseId) {
        prosessTaskData.setProperty(HENDELSE_ID, hendelseId);
    }

    public Optional<Long> getInngåendeHendelseId() {
        String propertyValue = prosessTaskData.getPropertyValue(INNGÅENDE_HENDELSE_ID);
        try {
            return Optional.of(Long.parseLong(propertyValue));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void setInngåendeHendelseId(Long inngåendeHendelseId) {
        prosessTaskData.setProperty(INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);
    }

    public Optional<String> getHendelseType() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_TYPE));
    }

    public void setHendelseType(String type) {
        prosessTaskData.setProperty(HENDELSE_TYPE, type);
    }

    public void setNesteKjøringEtter(LocalDateTime nesteKjøringEtter) {
        prosessTaskData.setNesteKjøringEtter(nesteKjøringEtter);
    }
}
