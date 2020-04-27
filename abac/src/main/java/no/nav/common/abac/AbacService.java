package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.Action;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;

import static no.nav.common.abac.XacmlRequestBuilder.*;

public class AbacService implements IAbacService {

    public final static String VEILARB_DOMAIN = "veilarb";

    private final AbacClient abacClient;

    private final String srvUsername;

    private final AuditLogger auditLogger;

    public AbacService(String abacUrl, String srvUsername, String srvPassword) {
        this.srvUsername = srvUsername;
        this.auditLogger = new AuditLogger();
        this.abacClient = new CachedAbacClient(new AbacHttpClient(abacUrl, srvUsername, srvPassword));
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
                lagAction(Action.ActionId.READ),
                lagVeilederAccessSubject(veilederIdent),
                lagEnhetResource(enhetId, VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException(String.format("%s mangler tilgang til enhet %s", veilederIdent, enhetId));
        }
    }

    @Override
    public void sjekkVeilederTilgangTilBruker(String veilederIdent, AbacPersonId personId) {
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(Action.ActionId.READ),
                lagVeilederAccessSubject(veilederIdent),
                lagPersonResource(personId, VEILARB_DOMAIN)
        );

        if (!harTilgang(xacmlRequest)) {
            throw new PepException("Veileder har ikke tilgang til bruker");
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
