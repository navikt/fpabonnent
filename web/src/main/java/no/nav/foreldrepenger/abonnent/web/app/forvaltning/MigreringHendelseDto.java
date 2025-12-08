package no.nav.foreldrepenger.abonnent.web.app.forvaltning;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.abonnent.felles.domene.HendelseType;
import no.nav.foreldrepenger.abonnent.felles.domene.HåndtertStatusType;

public record MigreringHendelseDto(@Size List<@Valid HendelseDto> hendelser) {

    public record HendelseDto(@Valid HendelseType type,
                              @Size @Pattern(regexp = "^[\\p{P}\\p{L}\\p{N}\\p{Alnum}\\p{Punct}\\p{Space}\\\\_.\\-]*$") String payload,
                              LocalDateTime haandteresEtter,
                              @Valid HåndtertStatusType haandtertStatus,
                              LocalDateTime sendtTid,
                              @Size @Pattern(regexp = "^[\\p{P}\\p{L}\\p{N}\\p{Alnum}\\p{Punct}\\p{Space}_.\\-]*$") String hendelseId,
                              @Size @Pattern(regexp = "^[\\p{P}\\p{L}\\p{N}\\p{Alnum}\\p{Punct}\\p{Space}_.\\-]*$") String tidligereHendelseId,
                              LocalDateTime opprettetTid) {}
}

