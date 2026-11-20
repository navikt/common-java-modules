package no.nav.common.client.aktoroppslag;

import java.util.List;
import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

@Value
public class BrukerIdenter {
    Fnr fnr;
    AktorId aktorId;
    List<Fnr> historiskeFnr;
    List<AktorId> historiskeAktorId;
}
