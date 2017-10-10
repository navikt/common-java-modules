package no.nav.dialogarena.aktor;

import java.util.Optional;

public interface AktorService {

    Optional<String> getFnr(String aktorId);

    Optional<String> getAktorId(String fnr);
}
