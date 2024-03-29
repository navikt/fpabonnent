package no.nav.foreldrepenger.abonnent.felles.tjeneste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;
import no.nav.foreldrepenger.abonnent.felles.domene.InngåendeHendelse;
import no.nav.vedtak.exception.TekniskException;

/**
 * Repository for InngåendeHendelse.
 * <p>
 * OBS1: Hvis du legger til nye spørringer er det viktig at de har HåndtertStatus som kriterie,
 * slik at de treffer riktig partisjon. Tabellen er partisjonert på denne statusen, der HÅNDTERT
 * ligger i den historiske (store) partisjonen som vi ikke tror det skal være behov for å spørre på.
 * <p>
 * OBS2: Du treffer ikke riktig index/partisjon hvis du spør på NOT en gitt status,
 * og heller ikke med status1 OR status2 (Oracle 12c R1).
 */
@ApplicationScoped
public class HendelseRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseRepository.class);

    private static final String SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT = "order by opprettetTidspunkt asc";

    private static final String HÅNDTERT_STATUS = "håndtertStatus";
    private static final String HENDELSE_KILDE = "hendelseKilde";
    private static final String HENDELSE_ID = "hendelseId";

    private EntityManager entityManager;

    HendelseRepository() {
        // for CDI proxy
    }

    @Inject
    public HendelseRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public InngåendeHendelse finnEksaktHendelse(Long inngåendeHendelseId) {
        return entityManager.find(InngåendeHendelse.class, inngåendeHendelseId);
    }

    public Optional<InngåendeHendelse> finnHendelseSomErSendtTilSortering(String hendelseId) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery("from InngåendeHendelse where hendelseId = :hendelseId " +
            "and håndtertStatus = :håndtertStatus " +
            SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT, InngåendeHendelse.class);
        query.setParameter(HENDELSE_ID, hendelseId);
        query.setParameter(HÅNDTERT_STATUS, HåndtertStatusType.SENDT_TIL_SORTERING);
        return queryTilOptional(hendelseId, query);
    }

    public Optional<InngåendeHendelse> finnHendelseFraIdHvisFinnes(String hendelseId, HendelseKilde hendelseKilde) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery("from InngåendeHendelse where hendelseKilde = :hendelseKilde " +
            "and hendelseId = :hendelseId ", InngåendeHendelse.class);
        query.setParameter(HENDELSE_KILDE, hendelseKilde);
        query.setParameter(HENDELSE_ID, hendelseId);
        return queryTilOptional(hendelseId, query);
    }

    public Optional<InngåendeHendelse> finnSenereKjedetHendelseHvisStatusMottatt(String hendelseId) {
        TypedQuery<InngåendeHendelse> query = entityManager
            .createQuery("from InngåendeHendelse where håndtertStatus = :håndtertStatus and tidligereHendelseId = :hendelseId", InngåendeHendelse.class);
        query.setParameter(HÅNDTERT_STATUS, HåndtertStatusType.MOTTATT);
        query.setParameter(HENDELSE_ID, hendelseId);
        return query.getResultList().stream().findFirst();
    }

    private Optional<InngåendeHendelse> queryTilOptional(String hendelseId, TypedQuery<InngåendeHendelse> query) {
        List<InngåendeHendelse> resultater = query.getResultList();
        if (resultater.size() > 1) {
            LOGGER.warn(new TekniskException("FP-164340", String.format("Fant mer enn en InngåendeHendelse med hendelseId=%s.", hendelseId)).getMessage());
        } else if (resultater.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resultater.getFirst());
    }

    public void lagreInngåendeHendelse(InngåendeHendelse inngåendeHendelse) {
        entityManager.persist(inngåendeHendelse);
    }

    public void lagreFlushInngåendeHendelse(InngåendeHendelse inngåendeHendelse) {
        entityManager.persist(inngåendeHendelse);
        entityManager.flush();
    }

    public void oppdaterHåndtertStatus(InngåendeHendelse inngåendeHendelse, HåndtertStatusType håndtertStatus) {
        inngåendeHendelse.setHåndtertStatus(håndtertStatus);
    }

    public void oppdaterHåndteresEtterTidspunkt(InngåendeHendelse inngåendeHendelse, LocalDateTime håndteresEtterTidspunkt) {
        inngåendeHendelse.setHåndteresEtterTidspunkt(håndteresEtterTidspunkt);
    }

    public void fjernPayload(InngåendeHendelse inngåendeHendelse) {
        inngåendeHendelse.setPayload(null);
    }

    public void markerHendelseSomSendtNå(InngåendeHendelse inngåendeHendelse) {
        inngåendeHendelse.setSendtTidspunkt(LocalDateTime.now());
    }

    public Optional<InngåendeHendelse> finnGrovsortertHendelse(HendelseKilde hendelseKilde, String hendelseId) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery("from InngåendeHendelse where hendelseKilde = :hendelseKilde " +
            "and hendelseId = :hendelseId " +
            "and håndtertStatus = :håndtertStatus " +
            SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT, InngåendeHendelse.class);
        query.setParameter(HENDELSE_KILDE, hendelseKilde);
        query.setParameter(HENDELSE_ID, hendelseId);
        query.setParameter(HÅNDTERT_STATUS, HåndtertStatusType.GROVSORTERT);

        List<InngåendeHendelse> resultater = query.getResultList();
        if (resultater.size() > 1) {
            LOGGER.warn(new TekniskException("FP-164339",
                String.format("Fant mer enn en InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", hendelseKilde.getKode(),
                    hendelseId, HåndtertStatusType.GROVSORTERT))
                .getMessage());
        } else if (resultater.isEmpty()) {
            LOGGER.warn(new TekniskException("FP-264339",
                String.format("Fant ikke InngåendeHendelse med hendelseKilde=%s, hendelseId=%s og håndtertStatus=%s.", hendelseKilde.getKode(),
                    hendelseId, HåndtertStatusType.GROVSORTERT)).getMessage());
            return Optional.empty();
        }
        return Optional.of(resultater.getFirst());
    }

    public int slettIrrelevanteHendelser() {
        // Vurder å bruke (payload is null or sendt_tid is null) - har slått av sjekk på sendt_tid pga sporing av korrigert/annullert
        int deletedRows = entityManager.createNativeQuery(
                "DELETE FROM INNGAAENDE_HENDELSE WHERE payload is null and haandtert_status = :handtert")
            .setParameter("handtert", HåndtertStatusType.HÅNDTERT.getKode())
            .executeUpdate();
        entityManager.flush();
        return deletedRows;
    }

    public int slettGamleHendelser() {
        // Kan justeres ned til mellom 0 dager og 60 dager - avhengig av behov for feilsøking.
        // 1/3 av endringer kommer samme dag, 2/3 innen 1 uke og 85-90% innen 30 dager
        // Logikken ser kun på tilfelle der tidligere hendelse ikke er håndtert
        // Dersom det ikke er behov for feilsøking: Kan slette alle som er håndtert (slå sammen med slettIrrelevanteHendelser)
        int deletedRows = entityManager.createNativeQuery(
                "DELETE FROM INNGAAENDE_HENDELSE WHERE sendt_tid < :foreldet and haandtert_status = :handtert")
            .setParameter("handtert", HåndtertStatusType.HÅNDTERT.getKode())
            .setParameter("foreldet", LocalDateTime.now().minusWeeks(10))
            .executeUpdate();
        entityManager.flush();
        return deletedRows;
    }

    public int slettGamleHendelser2() {
        // Kan justeres ned til mellom 0 dager og 60 dager - avhengig av behov for feilsøking.
        // 1/3 av endringer kommer samme dag, 2/3 innen 1 uke og 85-90% innen 30 dager
        // Logikken ser kun på tilfelle der tidligere hendelse ikke er håndtert
        // Dersom det ikke er behov for feilsøking: Kan slette alle som er håndtert (slå sammen med slettIrrelevanteHendelser)
        int deletedRows = entityManager.createNativeQuery(
                "DELETE FROM INNGAAENDE_HENDELSE WHERE sendt_tid is null and opprettet_tid < :foreldet and haandtert_status = :handtert")
            .setParameter("handtert", HåndtertStatusType.HÅNDTERT.getKode())
            .setParameter("foreldet", LocalDateTime.now().minusWeeks(10))
            .executeUpdate();
        entityManager.flush();
        return deletedRows;
    }

    public List<InngåendeHendelse> hentAlleInngåendeHendelser() {
        return entityManager.createQuery("from InngåendeHendelse", InngåendeHendelse.class).getResultList();
    }
}
