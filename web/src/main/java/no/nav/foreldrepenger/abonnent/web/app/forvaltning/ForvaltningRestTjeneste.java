package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.function.Function;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.abonnent.felles.task.VurderSorteringTask;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltning")
@RequestScoped
@Transactional
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ForvaltningRestTjeneste {

    private ProsessTaskTjeneste taskTjeneste;
    private HendelseRepository hendelseRepository;

    private Validator validator;

    public ForvaltningRestTjeneste() {
        // CDI
    }

    @Inject
    public ForvaltningRestTjeneste(ProsessTaskTjeneste taskTjeneste, HendelseRepository hendelseRepository) {
        this.taskTjeneste = taskTjeneste;
        this.hendelseRepository = hendelseRepository;
        @SuppressWarnings("resource") var factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @GET
    @Operation(description = "Leser ut hendelser som skal migreres", tags = "Forvaltning",
        summary = ("Leser ut hendelser som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Hendelser")})
    @Path("/lesHendelser")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response lesHendelser() {
        var hendelser = hendelseRepository.hentAlleInngåendeHendelser().stream()
            .map(MigreringMapper::tilHendelseDto)
            .toList();
        var respons = new MigreringHendelseDto(hendelser);
        var violations = validator.validate(respons);
        if (!violations.isEmpty()) {
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("Valideringsfeil; " + allErrors);
        }
        return Response.ok(respons).build();
    }

    @POST
    @Operation(description = "Lagrer hendelser som skal migreres", tags = "Forvaltning",
        summary = ("Lagre hendelser som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Hendelser")})
    @Path("/lagreHendelser")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response lagreHendelser(@TilpassetAbacAttributt(supplierClass = MigreringAbacSupplier.class)
                                       @NotNull @Parameter(name = "hendelser") @Valid MigreringHendelseDto hendelser) {
        hendelser.hendelser().stream()
            .map(MigreringMapper::fraHendelseDto)
            .forEach(h -> hendelseRepository.lagreInngåendeHendelse(h));
        return Response.ok().build();
    }

    @POST
    @Operation(description = "Lagre tasks som skal migreres", tags = "Forvaltning",
        summary = ("Lagre tasks som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Tasks")})
    @Path("/lagreTasks")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response lagreTasks(@TilpassetAbacAttributt(supplierClass = MigreringAbacSupplier.class)
                                   @NotNull @Parameter(name = "tasks") @Valid MigreringProsesstaskDto tasks) {
        tasks.tasks().stream()
            .map(MigreringMapper::fraProsesstaskDto)
            .forEach(t -> taskTjeneste.lagre(t));
        return Response.ok().build();
    }

    @GET
    @Operation(description = "Leser ut tasks som skal migreres", tags = "Forvaltning",
        summary = ("Leser ut tasks som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Tasks")})
    @Path("/lesTasks")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public Response lesTasks() {
        var vurderSortering = TaskType.forProsessTask(VurderSorteringTask.class);
        var tasks = taskTjeneste.finnAlle(ProsessTaskStatus.KLAR).stream()
            .filter(t -> vurderSortering.equals(t.taskType()))
            .map(MigreringMapper::tilProsesstaskDto)
            .toList();
        var respons = new MigreringProsesstaskDto(tasks);
        var violations = validator.validate(respons);
        if (!violations.isEmpty()) {
            var allErrors = violations.stream().map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("Valideringsfeil; " + allErrors);
        }
        return Response.ok(respons).build();
    }


    public static class MigreringAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
