package no.nav.foreldrepenger.abonnent.web.app.konfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import no.nav.foreldrepenger.abonnent.web.app.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.abonnent.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.abonnent.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.abonnent.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.abonnent.web.app.jackson.JacksonJsonConfig;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

@ApplicationPath(ApplicationConfig.API_URI)
public class ApplicationConfig extends Application {

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
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // eksponert grensesnitt
        classes.add(ProsessTaskRestTjeneste.class);

        // swagger
        classes.add(OpenApiResource.class);

        // Applikasjonsoppsett
        classes.add(JacksonJsonConfig.class);

        // ExceptionMappers pga de som finnes i Jackson+Jersey-media
        classes.add(ConstraintViolationMapper.class);
        classes.add(JsonMappingExceptionMapper.class);
        classes.add(JsonParseExceptionMapper.class);

        // Generell exceptionmapper m/logging for øvrige tilfelle
        classes.add(GeneralRestExceptionMapper.class);

        return Collections.unmodifiableSet(classes);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }
}
