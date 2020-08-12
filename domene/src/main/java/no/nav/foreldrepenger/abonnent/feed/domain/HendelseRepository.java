package no.nav.foreldrepenger.abonnent.feed.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;

/**
 * Repository for InngåendeHendelse.
 *
 * OBS1: Hvis du legger til nye spørringer er det viktig at de har HåndtertStatus som kriterie,
 * slik at de treffer riktig partisjon. Tabellen er partisjonert på denne statusen, der HÅNDTERT
 * ligger i den historiske (store) partisjonen som vi ikke tror det skal være behov for å spørre på.
 *
 * OBS2: Du treffer ikke riktig index/partisjon hvis du spør på NOT en gitt status,
 * og heller ikke med status1 OR status2 (Oracle 12c R1).
 */
@ApplicationScoped
public class HendelseRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseRepository.class);

    private static final String SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT = "order by opprettetTidspunkt asc"; //$NON-NLS-1$

    private static final String HÅNDTERT_STATUS = "håndtertStatus";
    private static final String FEED_KODE = "feedKode";
    private static final String REQUEST_UUID = "requestUuid";
    private static final String HENDELSE_ID = "hendelseId";

    private EntityManager entityManager;

    HendelseRepository() {
        // for CDI proxy
    }

    @Inject
    public HendelseRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public List<InngåendeHendelse> finnHendelserSomErSendtTilSorteringMedRequestUUID(String requestUUID) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery(
                "from InngåendeHendelse where requestUuid = :requestUuid " + //$NON-NLS-1$
                        "and håndtertStatus = :håndtertStatus " + //$NON-NLS-1$
                        SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT, InngåendeHendelse.class);
        query.setParameter(REQUEST_UUID, requestUUID);
        query.setParameter(HÅNDTERT_STATUS, HåndtertStatusType.SENDT_TIL_SORTERING);
        return query.getResultList();
    }

    public InngåendeHendelse finnEksaktHendelse(Long inngåendeHendelseId) {
        return entityManager.find(InngåendeHendelse.class, inngåendeHendelseId);
    }

    public Optional<InngåendeHendelse> finnHendelseFraIdHvisFinnes(String hendelseId, FeedKode feedKode) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery(
                "from InngåendeHendelse where feedKode = :feedKode " + //$NON-NLS-1$
                        "and hendelseId = :hendelseId ", InngåendeHendelse.class); //$NON-NLS-1$
        query.setParameter(FEED_KODE, feedKode);
        query.setParameter(HENDELSE_ID, hendelseId);

        List<InngåendeHendelse> resultater = query.getResultList();
        if (resultater.size() > 1) {
            LOGGER.warn(HendelseRepositoryFeil.FACTORY.fantMerEnnEnHendelse(feedKode.getKode(), hendelseId).getFeilmelding());
        } else if (resultater.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resultater.get(0));
    }

    public void lagreInngåendeHendelse(InngåendeHendelse inngåendeHendelse) {
        entityManager.persist(inngåendeHendelse);
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

    public Optional<InngåendeHendelse> finnGrovsortertHendelse(FeedKode feedKode, String hendelseId) {
        TypedQuery<InngåendeHendelse> query = entityManager.createQuery(
                "from InngåendeHendelse where feedKode = :feedKode " + //$NON-NLS-1$
                        "and hendelseId = :hendelseId " + //$NON-NLS-1$
                        "and håndtertStatus = :håndtertStatus " + //$NON-NLS-1$
                        SORTER_STIGENDE_PÅ_OPPRETTET_TIDSPUNKT, InngåendeHendelse.class);
        query.setParameter(FEED_KODE, feedKode);
        query.setParameter(HENDELSE_ID, hendelseId);
        query.setParameter(HÅNDTERT_STATUS, HåndtertStatusType.GROVSORTERT);

        List<InngåendeHendelse> resultater = query.getResultList();
        if (resultater.size() > 1) {
            LOGGER.warn(HendelseRepositoryFeil.FACTORY.fantMerEnnEnHendelseMedStatus(feedKode.getKode(), hendelseId, HåndtertStatusType.GROVSORTERT).getFeilmelding());
        } else if (resultater.isEmpty()) {
            LOGGER.warn(HendelseRepositoryFeil.FACTORY.fantIkkeHendelse(feedKode.getKode(), hendelseId, HåndtertStatusType.GROVSORTERT).getFeilmelding());
            return Optional.empty();
        }
        return Optional.of(resultater.get(0));
    }
}
