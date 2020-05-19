package no.nav.foreldrepenger.abonnent.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.abonnent.dbstøtte.UnittestRepositoryRule;
import no.nav.foreldrepenger.abonnent.felles.RequestContextHandler;
import no.nav.foreldrepenger.abonnent.kodeverdi.FeedKode;
import no.nav.foreldrepenger.abonnent.kodeverdi.HendelseType;
import no.nav.foreldrepenger.abonnent.kodeverdi.HåndtertStatusType;
import no.nav.vedtak.felles.jpa.TransactionHandler;

@Ignore // Feiler innimellom på GHA. Nå som formula-annotasjon på HåndtertStatusType i InngåendeHendelse
// er borte kan låsingen trolig gå direkte mot InngåendeHendelse, og bør derfor skrives om - TFP-2932
public class HendelseRepositoryLockTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseRepositoryLockTest.class);

    private static final int ANTALL_TRÅDER = 5;
    private Long sekvensnummer = 1000L;
    private String trådSomFikkLåsen;
    private volatile AtomicInteger teller = new AtomicInteger(0);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private HendelseRepository hendelseRepository = new HendelseRepository(repoRule.getEntityManager());

    @Before
    public void before() throws Exception {
        slettHendelser();
    }

    @After
    public void after() throws Exception {
        slettHendelser();
    }

    @Test
    public void skal_bare_slippe_inn_en_tråd_i_grovsortering_om_gangen() throws Exception {
        // Arrange
        List<TestTråd> testTråder = new ArrayList<>();

        // Act
        for (int i = 1; i <= ANTALL_TRÅDER; i++) {
            TestTråd testTråd = new TestTråd();
            testTråder.add(testTråd);
            testTråd.start();
        }
        for (TestTråd testTråd : testTråder) {
            testTråd.join();
        }

        // Assert
        List<InngåendeHendelse> resultat = repoRule.getEntityManager()
                .createQuery("from InngåendeHendelse order by id", InngåendeHendelse.class).getResultList();
        assertThat(resultat).hasSize(ANTALL_TRÅDER + 1); // 5 hendelser pr tråd, 1 hendelse for tråden som fikk lås
        assertThat(resultat.get(ANTALL_TRÅDER).getPayload()).isEqualTo(trådSomFikkLåsen);
    }

    private final class TestTråd extends Thread {
        private Void doStart() {
            try {
                new UtførTestenITransaksjon().doWork();
            } catch (Exception e) {
            } // NOSONAR
            return null;
        }

        public void run() {
            try {
                RequestContextHandler.doWithRequestContext(this::doStart);
            } catch (Exception e) {
            } // NOSONAR
        }
    }

    private final class UtførTestenITransaksjon extends TransactionHandler<Void> {
        Void doWork() throws Exception {
            try {
                return super.apply(repoRule.getEntityManager());
            } finally {
                CDI.current().destroy(repoRule.getEntityManager());
            }
        }

        @Override
        protected Void doWork(EntityManager entityManager) throws Exception {
            try {
                lagHendelse(LocalDateTime.now().minusMinutes(1), Thread.currentThread().getName()); // Hendelse som skal
                                                                                                    // leses opp igjen

                List<InngåendeHendelse> inngåendeHendelser = hendelseRepository
                        .finnHendelserSomErKlareTilGrovsortering();
                if (!inngåendeHendelser.isEmpty()) {
                    trådSomFikkLåsen = Thread.currentThread().getName();
                    LOGGER.info(trådSomFikkLåsen + " fikk låsen");
                    while (teller.get() < ANTALL_TRÅDER - 1) {
                        Thread.onSpinWait();
                    }
                    lagHendelse(LocalDateTime.now(), trådSomFikkLåsen); // Hendelse som brukes til å asserte at bare en
                                                                        // tråd fikk lås
                } else {
                    LOGGER.info(Thread.currentThread().getName() + " fikk IKKE låsen");
                    teller.incrementAndGet();
                }

            } finally {
                CDI.current().destroy(entityManager);
            }
            return null;
        }

        private void lagHendelse(LocalDateTime now, String trådnavn) {
            InngåendeHendelse hendelse = InngåendeHendelse.builder()
                    .hendelseId("" + sekvensnummer++)
                    .type(HendelseType.FØDSELSMELDINGOPPRETTET)
                    .payload(trådnavn)
                    .feedKode(FeedKode.TPS)
                    .requestUuid("req_uuid")
                    .håndtertStatus(HåndtertStatusType.MOTTATT)
                    .håndteresEtterTidspunkt(now)
                    .build();
            hendelseRepository.lagreInngåendeHendelse(hendelse);
        }
    }

    private void slettHendelser() throws Exception {
        SletteTråd slettetråd = new SletteTråd();
        slettetråd.start();
        slettetråd.join();
    }

    private final class SletteTråd extends Thread {
        private Void doStart() {
            try {
                new UtførSlettingITransaksjon().doWork();
            } catch (Exception e) {
            } // NOSONAR
            return null;
        }

        public void run() {
            try {
                RequestContextHandler.doWithRequestContext(this::doStart);
            } catch (Exception e) {
            } // NOSONAR
        }
    }

    private final class UtførSlettingITransaksjon extends TransactionHandler<Void> {
        Void doWork() throws Exception {
            try {
                return super.apply(repoRule.getEntityManager());
            } finally {
                CDI.current().destroy(repoRule.getEntityManager());
            }
        }

        @Override
        protected Void doWork(EntityManager entityManager) {
            try {
                repoRule.getEntityManager().createQuery("delete from InngåendeHendelse").executeUpdate();
            } finally {
                CDI.current().destroy(entityManager);
            }
            return null;
        }
    }
}
