package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;

import javax.inject.Inject;


public class PepClient {

    private final Pep pep;
    private final String applicationDomain;

    public PepClient(Pep pep, String applicationDomain) {
        this.pep = pep;
        this.applicationDomain = applicationDomain;
    }

    @SneakyThrows
    public String sjekkTilgangTilFnr(String fnr) {
        if (Decision.Permit == pep.isServiceCallAllowedWithOidcToken(getToken(), applicationDomain, fnr).getBiasedDecision()) {
            return fnr;
        } else {
            throw new IngenTilgang();
        }
    }

    private String getToken() {
        return SubjectHandler.getSubjectHandler().getSubject()
                .getPublicCredentials()
                .stream()
                .filter(o -> o instanceof OidcCredential)
                .map(o -> (OidcCredential) o)
                .findFirst()
                .map(OidcCredential::getToken)
                .orElseThrow(IngenTilgang::new);
    }

}
