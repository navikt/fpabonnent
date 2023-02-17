package no.nav.foreldrepenger.abonnent.web.app.tjenester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Starter interne applikasjontjenester
 */
@WebListener
public class ApplicationContextListener implements ServletContextListener {

    @Inject
    private ApplicationServiceStarter applicationServiceStarter; //NOSONAR - vil ikke fungere med constructor innjection

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        applicationServiceStarter.startServices();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        applicationServiceStarter.stopServices();
    }
}
