package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseKilde;
import no.nav.foreldrepenger.abonnent.felles.tjeneste.HendelseRepository;
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

    private HendelseRepository hendelseRepository;

    private Validator validator;

    public ForvaltningRestTjeneste() {
        // CDI
    }

    @Inject
    public ForvaltningRestTjeneste(HendelseRepository hendelseRepository) {
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
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

    @GET
    @Operation(description = "Leser ut hendelser som skal migreres", tags = "Forvaltning",
        summary = ("Leser ut hendelser som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Hendelser")})
    @Path("/lesEnkeltHendelse")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response lesEnkeltHendelse(@TilpassetAbacAttributt(supplierClass = MigreringAbacSupplier.class)
                                          @NotNull @QueryParam("hendelseId") @Parameter(description = "hendelseId") @Valid UUID hendelseId) {
        var hendelser = hendelseRepository.finnHendelseFraIdHvisFinnes(hendelseId.toString(), HendelseKilde.PDL).stream()
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
    @Operation(description = "Sammenlign hendelser som skal migreres", tags = "Forvaltning",
        summary = ("Sammenlign hendelser som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Hendelser")})
    @Path("/sammenlignHendelser")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response sammenlignHendelser(@TilpassetAbacAttributt(supplierClass = MigreringAbacSupplier.class)
                                   @NotNull @Parameter(name = "hendelser") @Valid MigreringHendelseDto hendelser) {
        var rmap = hendelser.hendelser().stream()
            .map(MigreringMapper::fraHendelseDto)
            .toList();
        var lokale = hendelser.hendelser().stream()
            .map(h -> hendelseRepository.finnHendelseFraIdHvisFinnes(h.hendelseId(), HendelseKilde.PDL))
            .flatMap(Optional::stream)
            .map(MigreringMapper::tilHendelseDto)
            .collect(Collectors.toSet());
        var remote = new HashSet<>(hendelser.hendelser());
        return lokale.size() == remote.size() && lokale.containsAll(remote) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Operation(description = "Lagrer hendelser som skal migreres", tags = "Forvaltning",
        summary = ("Lagre hendelser som skal migreres"),
        responses = {@ApiResponse(responseCode = "200", description = "Hendelser")})
    @Path("/lagreHendelser")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response lagreHendelser(@TilpassetAbacAttributt(supplierClass = MigreringAbacSupplier.class)
                                       @NotNull @Parameter(name = "hendelser") @Valid MigreringHendelseDto hendelser) {
        hendelser.hendelser().forEach(this::lagreEllerOppdater);
        return Response.ok().build();
    }

    private void lagreEllerOppdater(MigreringHendelseDto.HendelseDto hendelseDto) {
        var eksisterende = hendelseRepository.finnHendelseFraIdHvisFinnes(hendelseDto.hendelseId(), HendelseKilde.PDL).orElse(null);
        if (eksisterende != null) {
            /* Ikke i runde 1 i mottak */
            eksisterende.setHåndtertStatus(hendelseDto.haandtertStatus());
            eksisterende.setHåndteresEtterTidspunkt(hendelseDto.haandteresEtter());
            eksisterende.setSendtTidspunkt(hendelseDto.sendtTid());
            hendelseRepository.lagreInngåendeHendelse(eksisterende);
        } else {
            var nyHendelse = MigreringMapper.fraHendelseDto(hendelseDto);
            hendelseRepository.lagreInngåendeHendelse(nyHendelse);
        }
    }

    public static class MigreringAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
