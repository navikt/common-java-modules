package no.nav.common.client.aktorregister;

import java.util.List;

public interface AktorregisterClient {

    String hentFnr(String aktorId);

    String hentAktorId(String fnr);

    List<IdentOppslag> hentFnr(List<String> aktorIdListe);

    List<IdentOppslag> hentAktorId(List<String> fnrListe);

}
