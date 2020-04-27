package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;

import static no.nav.common.abac.XacmlRequestBuilder.*;

public class AbacService implements Pep {

    public final static String VEILARB_DOMAIN = "veilarb";

    private final AbacClient abacClient;

    private final String srvUsername;

    private final AuditLogger auditLogger;

    public AbacService(String abacUrl, String srvUsername, String srvPassword) {
        this.srvUsername = srvUsername;
        this.auditLogger = new AuditLogger();
        this.abacClient = new AbacCachedClient(new AbacHttpClient(abacUrl, srvUsername, srvPassword));
    }

    public AbacService(String srvUsername, AbacClient abacClient, AuditLogger auditLogger) {
        this.srvUsername = srvUsername;
        this.abacClient = abacClient;
        this.auditLogger = auditLogger;
    }

    @Override
    public void sjekkVeilederTilgangTilEnhet(String veilederIdent, String enhetId) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(ActionId.READ),
                lagVeilederAccessSubject(veilederIdent),
                lagEnhetResource(enhetId, VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til enhet");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilBruker(String veilederIdent, ActionId actionId, AbacPersonId personId) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                lagPersonResource(personId, VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til bruker");
        }
    }

    @Override
    public void sjekkTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                lagPersonResource(personId, VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Innlogget bruker har ikke tilgang til person");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilKode6(String veilederIdent) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                lagKode6Resource(VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til kode 6");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilKode7(String veilederIdent) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                lagKode7Resource(VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til kode 7");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilEgenAnsatt(String veilederIdent) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                lagEgenAnsattResource(VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til egen ansatt");
        }
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }

    private boolean harTilgang(XacmlRequest xacmlRequest) {
        auditLogger.logRequestInfo(xacmlRequest.getRequest());

        String xacmlRequestJson = XacmlMapper.mapRequestToEntity(xacmlRequest);
        String xacmlResponseJson = abacClient.sendRequest(xacmlRequestJson);
        XacmlResponse xacmlResponse = XacmlMapper.mapRawResponse(xacmlResponseJson);

        Decision decision = XacmlResponseParser.getSingleDecision(xacmlResponse);

        auditLogger.logResponseInfo(decision.name(), xacmlResponse, xacmlRequest.getRequest());

        return XacmlResponseParser.harTilgang(xacmlResponse);
    }

}
