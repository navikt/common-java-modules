package no.nav.common.abac;

import no.nav.common.abac.cef.CefAbacEventContext;
import no.nav.common.abac.cef.CefAbacResponseMapper;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;
import no.nav.common.auth.subject.SubjectHandler;

import static no.nav.common.abac.XacmlRequestBuilder.*;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

public class VeilarbPep implements Pep {

    public final static String VEILARB_DOMAIN = "veilarb";

    private final AbacClient abacClient;

    private final String srvUsername;

    private final AuditLogger auditLogger;

    public VeilarbPep(String abacUrl, String srvUsername, String srvPassword) {
        this.srvUsername = srvUsername;
        this.auditLogger = new AuditLogger();
        this.abacClient = new AbacCachedClient(new AbacHttpClient(abacUrl, srvUsername, srvPassword));
    }

    public VeilarbPep(String srvUsername, AbacClient abacClient, AuditLogger auditLogger) {
        this.srvUsername = srvUsername;
        this.abacClient = abacClient;
        this.auditLogger = auditLogger;
    }

    @Override
    public void sjekkVeilederTilgangTilEnhet(String veilederIdent, String enhetId, RequestInfo requestInfo) {
        ActionId actionId = ActionId.READ;
        Resource resource = lagEnhetResource(enhetId, VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.enhetIdMapper(enhetId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til enhet");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilBruker(String veilederIdent, ActionId actionId, AbacPersonId personId, RequestInfo requestInfo) {
        Resource resource = lagPersonResource(personId, VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(personId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til bruker");
        }
    }

    @Override
    public void sjekkTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId, RequestInfo requestInfo) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagPersonResource(personId, VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(personId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, SubjectHandler.getIdent().orElse(null));

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Innlogget bruker har ikke tilgang til person");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilKode6(String veilederIdent, RequestInfo requestInfo) {
        Resource resource = lagKode6Resource(VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til kode 6");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilKode7(String veilederIdent, RequestInfo requestInfo) {
        Resource resource = lagKode7Resource(VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til kode 7");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilEgenAnsatt(String veilederIdent, RequestInfo requestInfo) {
        Resource resource = lagEgenAnsattResource(VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, requestInfo, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til egen ansatt");
        }
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }

    private boolean harTilgang(XacmlRequest xacmlRequest, CefAbacEventContext cefEventContext) {
        XacmlResponse xacmlResponse = abacClient.sendRequest(xacmlRequest);

        auditLogger.logCEF(xacmlRequest, xacmlResponse, cefEventContext);

        return XacmlResponseParser.harTilgang(xacmlResponse);
    }

    private CefAbacEventContext lagCefEventContext(CefAbacResponseMapper mapper, RequestInfo requestInfo, String subjectId) {
        return CefAbacEventContext.builder()
                .applicationName(requireApplicationName())
                .callId(requestInfo.callId)
                .consumerId(requestInfo.consumerId)
                .requestMethod(requestInfo.requestMethod)
                .requestPath(requestInfo.requestPath)
                .subjectId(subjectId)
                .mapper(mapper)
                .build();
    }
}
