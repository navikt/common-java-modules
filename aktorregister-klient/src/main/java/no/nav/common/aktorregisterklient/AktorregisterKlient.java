package no.nav.common.aktorregisterklient;

import java.util.List;

public interface AktorregisterKlient {

    String hentFnr(String aktorId);

    String hentAktorId(String fnr);

    List<IdentOppslag> hentFnr(List<String> aktorIdListe);

    List<IdentOppslag> hentAktorId(List<String> fnrListe);

}
