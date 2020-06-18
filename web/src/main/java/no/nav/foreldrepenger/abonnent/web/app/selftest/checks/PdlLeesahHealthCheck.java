package no.nav.foreldrepenger.abonnent.web.app.selftest.checks;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;

import no.nav.foreldrepenger.abonnent.pdl.PdlLeesahHendelseStream;

@ApplicationScoped
public class PdlLeesahHealthCheck extends ExtHealthCheck {

    private PdlLeesahHendelseStream hendelseStream;

    PdlLeesahHealthCheck() {
    }

    @Inject
    public PdlLeesahHealthCheck(PdlLeesahHendelseStream hendelseStream) {
        this.hendelseStream = hendelseStream;
    }

    @Override
    protected String getDescription() {
        return "Kafka hendelse-stream fra PDL/LEESAH.";
    }

    @Override
    protected String getEndpoint() {
        return hendelseStream.getTopicName();
    }

    @Override
    protected InternalResult performCheck() {
        InternalResult intTestRes = new InternalResult();

        KafkaStreams.State tilstand = hendelseStream.getTilstand();
        intTestRes.setMessage("Consumer is in state [" + tilstand.name() + "].");
        if (tilstand.isRunningOrRebalancing() || KafkaStreams.State.CREATED.equals(tilstand)) {
            intTestRes.setOk(true);
        } else {
            intTestRes.setOk(false);
        }
        intTestRes.noteResponseTime();

        return intTestRes;
    }
}
