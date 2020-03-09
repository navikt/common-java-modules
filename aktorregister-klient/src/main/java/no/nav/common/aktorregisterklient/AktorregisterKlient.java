package no.nav.common.aktorregisterklient;

import java.util.List;
import java.util.Optional;

public interface AktorregisterKlient {

    Optional<String> hentFnr(String aktorId);

    Optional<String> hentAktorId(String fnr);

    List<IdentOppslag> hentFnr(List<String> aktorIdListe);

    List<IdentOppslag> hentAktorId(List<String> fnrListe);

}
