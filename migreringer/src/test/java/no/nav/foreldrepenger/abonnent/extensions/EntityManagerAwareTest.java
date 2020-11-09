package no.nav.foreldrepenger.abonnent.extensions;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FPabonnentEntityManagerAwareExtension.class)
public abstract class EntityManagerAwareTest extends no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest {
}
