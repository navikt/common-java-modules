package no.nav.common.client.aktoroppslag;

import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.List;

@Value
public class BrukerIdenter {
    Fnr fnr;
    AktorId aktorId;
    List<Fnr> historiskeFnr;
    List<AktorId> historiskeAktorId;
}
