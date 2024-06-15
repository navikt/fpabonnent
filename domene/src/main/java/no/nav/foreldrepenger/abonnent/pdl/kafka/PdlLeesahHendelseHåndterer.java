package no.nav.foreldrepenger.abonnent.pdl.kafka;

import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.DØD;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.DØDFØDSEL;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.FØDSEL;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.FØDSELSDATO;
import static no.nav.foreldrepenger.abonnent.pdl.kafka.PdlLeesahOversetter.UTFLYTTING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.foreldrepenger.abonnent.felles.task.HendelserDataWrapper;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.foreldrepenger.abonnent.pdl.domene.eksternt.PdlPersonhendelse;
import no.nav.foreldrepenger.abonnent.pdl.kafka.test.VtpKafkaAvroDeserializer;
import no.nav.foreldrepenger.abonnent.pdl.tjeneste.ForsinkelseTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer implements KafkaMessageHandler<String, Personhendelse> {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseHåndterer.class);
    private static final Environment ENV = Environment.current();
    private static final Map<String, Object> SCHEMA_MAP = getSchemaMap();

    private HendelseRepository hendelseRepository;
    private PdlLeesahOversetter oversetter;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ForsinkelseTjeneste forsinkelseTjeneste;
    private String topicName;

    PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(HendelseRepository hendelseRepository,
                                      PdlLeesahOversetter pdlLeesahOversetter,
                                      ProsessTaskTjeneste prosessTaskTjeneste,
                                      ForsinkelseTjeneste forsinkelseTjeneste,
                                      @KonfigVerdi(value = "kafka.pdl.leesah.topic") String topicName) {
        this.hendelseRepository = hendelseRepository;
        this.oversetter = pdlLeesahOversetter;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.forsinkelseTjeneste = forsinkelseTjeneste;
        this.topicName = topicName;
    }

    @Override
    public void handleRecord(String key, Personhendelse value) {
        handleMessage(key, value);
    }


    @SuppressWarnings("unused")
    void handleMessage(String key, Personhendelse payload) { // key er spesialtegn + aktørId, som også finnes i payload
        setCallIdForHendelse(payload);

        var inngåendeHendelse = hendelseRepository.finnHendelseFraIdHvisFinnes(payload.getHendelseId().toString(), HendelseKilde.PDL);
        if (inngåendeHendelse.isPresent()) {
            LOG.info(
                "FPABONNENT mottok duplikat hendelse som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}. Tiltak: Sjekk om det skjedde en deploy/restart av Fpabonnent i det samme tidsrommet - i så fall kan dette ignoreres.",
                payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
                payload.getTidligereHendelseId());
            return;
        }

        if (FØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterFødsel(payload);
        } else if (FØDSELSDATO.contentEquals(payload.getOpplysningstype())) {
            håndterFødselsdato(payload);
        } else if (DØD.contentEquals(payload.getOpplysningstype())) {
            håndterDødsfall(payload);
        } else if (DØDFØDSEL.contentEquals(payload.getOpplysningstype())) {
            håndterDødfødtBarn(payload);
        } else if (UTFLYTTING.contentEquals(payload.getOpplysningstype())) {
            håndterUtflytting(payload);
        }
    }

    private void håndterFødsel(Personhendelse payload) {
        var foedsel = payload.getFoedsel();
        if (foedsel != null) {
            loggMottakMedDato(payload, "fødsel", "fødselsdato", foedsel.getFoedselsdato());
        } else {
            loggMottakUtenDato(payload, "fødsel");
        }
        var pdlFødsel = oversetter.oversettFødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlFødsel);
    }

    private void håndterFødselsdato(Personhendelse payload) {
        var foedselsdato = payload.getFoedselsdato();
        if (foedselsdato != null) {
            loggMottakMedDato(payload, "fødsel dato", "fødselsdato", foedselsdato.getFoedselsdato());
        } else {
            loggMottakUtenDato(payload, "fødsel dato");
        }
        //var pdlFødsel = oversetter.oversettFødsel(payload);
        //prosesserHendelseVidereHvisRelevant(pdlFødsel);
    }

    private void håndterDødsfall(Personhendelse payload) {
        var doedsfall = payload.getDoedsfall();
        if (doedsfall != null) {
            loggMottakMedDato(payload, "dødsfall", "dødsdato", doedsfall.getDoedsdato());
        } else {
            loggMottakUtenDato(payload, "dødsfall");
        }
        var pdlDød = oversetter.oversettDød(payload);
        prosesserHendelseVidereHvisRelevant(pdlDød);
    }

    private void håndterDødfødtBarn(Personhendelse payload) {
        var doedfoedtBarn = payload.getDoedfoedtBarn();
        if (doedfoedtBarn != null) {
            loggMottakMedDato(payload, "dødfødtBarn", "dødfødseldato", doedfoedtBarn.getDato());
        } else {
            loggMottakUtenDato(payload, "dødfødtBarn");
        }
        var pdlDødfødsel = oversetter.oversettDødfødsel(payload);
        prosesserHendelseVidereHvisRelevant(pdlDødfødsel);
    }

    private void håndterUtflytting(Personhendelse payload) {
        var utflytting = payload.getUtflyttingFraNorge();
        if (utflytting != null) {
            loggMottakMedDato(payload, "utflyttingFraNorge", "utflyttingsdato", utflytting.getUtflyttingsdato());
        } else {
            loggMottakUtenDato(payload, "utflyttingFraNorge");
        }
        var pdlUtflytting = oversetter.oversettUtflytting(payload);
        prosesserHendelseVidereHvisRelevant(pdlUtflytting);
    }

    private void loggMottakMedDato(Personhendelse payload, String hendelse, String datofelt, LocalDate dato) {
        LOG.info("FPABONNENT mottok {}: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={} {}={}",
            hendelse, payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
            payload.getTidligereHendelseId(), datofelt, dato);
    }

    private void loggMottakUtenDato(Personhendelse payload, String hendelse) {
        LOG.info("FPABONNENT mottok {}: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}", hendelse,
            payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(),
            payload.getTidligereHendelseId());
    }

    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId();
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }

    private void prosesserHendelseVidereHvisRelevant(PdlPersonhendelse personhendelse) {
        if (personhendelse.erRelevantForFpsak()) {
            LOG.info("Lagrer");
            var inngåendeHendelse = lagreInngåendeHendelse(personhendelse);
            LOG.info("Finner neste");
            var håndteresEtterTidspunkt = forsinkelseTjeneste.finnNesteTidspunktForVurderSortering(inngåendeHendelse);
            LOG.info("Opppdaterer");
            hendelseRepository.oppdaterHåndteresEtterTidspunkt(inngåendeHendelse, håndteresEtterTidspunkt);
            LOG.info("Oppretter");
            opprettVurderSorteringTask(inngåendeHendelse, håndteresEtterTidspunkt);
            LOG.info("Opprettet OK");
        }
    }

    private InngåendeHendelse lagreInngåendeHendelse(PdlPersonhendelse personhendelse) {
        var jsonPayload = DefaultJsonMapper.toJson(personhendelse);
        if (jsonPayload == null) {
            LOG.warn("Tom payload for objekt {}", personhendelse);
        }
        var inngåendeHendelse = InngåendeHendelse.builder()
            .hendelseType(personhendelse.getHendelseType())
            .hendelseId(personhendelse.getHendelseId())
            .tidligereHendelseId(personhendelse.getTidligereHendelseId())
            .payload(jsonPayload)
            .hendelseKilde(HendelseKilde.PDL)
            .håndtertStatus(HåndtertStatusType.MOTTATT)
            .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        return inngåendeHendelse;
    }

    private void opprettVurderSorteringTask(InngåendeHendelse inngåendeHendelse, LocalDateTime håndteresEtterTidspunkt) {
        var vurderSorteringTask = new HendelserDataWrapper(ProsessTaskData.forProsessTask(VurderSorteringTask.class));
        vurderSorteringTask.setHendelseId(inngåendeHendelse.getHendelseId());
        vurderSorteringTask.setNesteKjøringEtter(håndteresEtterTidspunkt);
        vurderSorteringTask.setHendelseType(inngåendeHendelse.getHendelseType().getKode());
        prosessTaskTjeneste.lagre(vurderSorteringTask.getProsessTaskData());
    }


    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public String groupId() { // Keep stable (or it will read from autoOffsetReset()
        return "fpabonnent";
    }

    @Override
    public Supplier<Deserializer<String>> keyDeserializer() {
        return () -> {
            var s = new StringDeserializer();
            s.configure(SCHEMA_MAP, true);
            return s;
        };
    }

    @Override
    public Supplier<Deserializer<Personhendelse>> valueDeserializer() {
        return () -> {
            var s = getDeserializer();
            s.configure(SCHEMA_MAP, false);
            return s;
        };
    }

    private static Deserializer<Personhendelse> getDeserializer() {
        return ENV.isProd() || ENV.isDev() ? new WrappedAvroDeserializer<>() : new WrappedAvroDeserializer<>(new VtpKafkaAvroDeserializer());
    }

    private static Map<String, Object> getSchemaMap() {
        var schemaRegistryUrl = KafkaProperties.getAvroSchemaRegistryURL();
        if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
            return Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, KafkaProperties.getAvroSchemaRegistryBasicAuth(),
                SPECIFIC_AVRO_READER_CONFIG, true);
        } else {
            return Map.of();
        }
    }
}
