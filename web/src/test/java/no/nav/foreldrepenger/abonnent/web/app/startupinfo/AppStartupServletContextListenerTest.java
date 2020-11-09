package no.nav.foreldrepenger.abonnent.web.app.startupinfo;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.servlet.ServletContextEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AppStartupServletContextListenerTest {

    private AppStartupServletContextListener listener; // objekter vi tester

    private AppStartupInfoLogger mockAppStartupInfoLogger;

    @BeforeEach
    public void setup() {
        listener = new AppStartupServletContextListener();
        mockAppStartupInfoLogger = mock(AppStartupInfoLogger.class);
        listener.setAppStartupInfoLogger(mockAppStartupInfoLogger);
    }

    @Test
    public void test_contextInitialized_ok() {
        listener.contextInitialized(mock(ServletContextEvent.class));
        verify(mockAppStartupInfoLogger).logAppStartupInfo();
    }

    @Test
    public void test_contextInitialized_exception() {
        doThrow(new RuntimeException("!")).when(mockAppStartupInfoLogger).logAppStartupInfo();
        listener.contextInitialized(mock(ServletContextEvent.class));
    }

    @Test
    public void test_contextDestroyed_ok() {
        listener.contextDestroyed(mock(ServletContextEvent.class));
    }
}
