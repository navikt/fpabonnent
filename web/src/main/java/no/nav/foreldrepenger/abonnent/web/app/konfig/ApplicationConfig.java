package no.nav.foreldrepenger.abonnent.web.app.konfig;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.abonnent.web.app.exceptions.KnownExceptionMappers;
import no.nav.foreldrepenger.abonnent.web.app.jackson.JacksonJsonConfig;
import no.nav.vedtak.felles.integrasjon.rest.jersey.TimingFilter;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends ResourceConfig {

    public static final String API_URI = "/api";

    public ApplicationConfig() {
        OpenAPI oas = new OpenAPI();
        Info info = new Info()
                .title("Vedtaksløsningen - Abonnent")
                .version("1.0")
                .description("REST grensesnitt for Fpabonnent.");

        oas.info(info)
                .addServersItem(new Server()
                        .url("/fpabonnent"));
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner")
                .resourcePackages(Stream.of("no.nav")
                        .collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(oasConfig)
                    .buildContext(true)
                    .read();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        register(OpenApiResource.class);
        register(JacksonJsonConfig.class);
        register(TimingFilter.class);

        registerClasses(Set.of(ProsessTaskRestTjeneste.class));

        registerInstances(new LinkedHashSet<>(new KnownExceptionMappers().getExceptionMappers()));

        property(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }
}
