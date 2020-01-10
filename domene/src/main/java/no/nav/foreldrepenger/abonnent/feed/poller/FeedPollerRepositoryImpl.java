package no.nav.foreldrepenger.abonnent.feed.poller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.abonnent.feed.domain.InputFeed;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class FeedPollerRepositoryImpl {
    private EntityManager entityManager;

    FeedPollerRepositoryImpl() {
        // for CDI proxy
    }

    @Inject
    public FeedPollerRepositoryImpl(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    Optional<InputFeed> finnInputFeed(FeedKode feedKode) {
        Query query = entityManager.createNativeQuery("select * from INPUT_FEED where kode = :feedKode for update skip locked", InputFeed.class);
        query.setParameter("feedKode", feedKode.getKode());

        List<InputFeed> resultater = query.getResultList();
        if (resultater.isEmpty()) {
            return Optional.empty();
        }
        if (resultater.size() == 1) {
            return Optional.of(resultater.get(0));
        }
        throw new IllegalArgumentException("Utviklerfeil, fikk flere enn ett resultat for INPUT_FEED med feedKode " + feedKode);
    }
}
