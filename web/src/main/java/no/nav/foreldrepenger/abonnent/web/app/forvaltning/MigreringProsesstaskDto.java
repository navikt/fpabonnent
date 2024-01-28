package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

public record MigreringProsesstaskDto(@Valid @Size List<TaskDto> tasks) {

    public record TaskDto(@Size @Valid Properties taskParametere, LocalDateTime nesteKjøringEtter) {}
}

