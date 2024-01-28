package no.nav.foreldrepenger.abonnent.felles.task;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

public class HendelserDataWrapper {

    public static final String HENDELSE_ID = "hendelse.id";
    public static final String HENDELSE_TYPE = "hendelse.type";
    @SuppressWarnings("unused")
    public static final String HENDELSE_KILDE = "hendelse.kilde"; // Ta i bruk ved taskoppretting når ny kilde eller omlegging. Nå kun PDL

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

    public Optional<String> getHendelseType() {
        return Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSE_TYPE));
    }

    public void setHendelseType(String type) {
        prosessTaskData.setProperty(HENDELSE_TYPE, type);
    }

    public Optional<HendelseKilde> getHendelseKilde() {
        return Optional.of(HendelseKilde.PDL);
    }

    public void setNesteKjøringEtter(LocalDateTime nesteKjøringEtter) {
        prosessTaskData.setNesteKjøringEtter(nesteKjøringEtter);
    }
}
