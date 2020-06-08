package no.nav.common.abac;

import no.nav.common.abac.cef.CefAbacEventContext;
import no.nav.common.abac.cef.CefAbacResponseMapper;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;

import java.util.Optional;

import static no.nav.common.abac.XacmlRequestBuilder.*;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

public class VeilarbPep implements Pep {

    private final AbacClient abacClient;

    private final String srvUsername;

    private final AuditLogger auditLogger;

    private final SubjectProvider subjectProvider;

    private final AuditRequestInfoSupplier auditRequestInfoSupplier;

    public VeilarbPep(String abacUrl, String srvUsername, String srvPassword) {
        this(abacUrl, srvUsername, srvPassword, null);
    }

    public VeilarbPep(String abacUrl,
                      String srvUsername,
                      String srvPassword,
                      AuditRequestInfoSupplier auditRequestInfoSupplier) {
        this.srvUsername = srvUsername;
        this.auditLogger = new AuditLogger();
        this.abacClient = new AbacCachedClient(new AbacHttpClient(abacUrl, srvUsername, srvPassword));
        this.subjectProvider = new NimbusSubjectProvider();
        this.auditRequestInfoSupplier = auditRequestInfoSupplier;
    }

    public VeilarbPep(String srvUsername,
                      AbacClient abacClient,
                      AuditLogger auditLogger,
                      SubjectProvider subjectProvider,
                      AuditRequestInfoSupplier auditRequestInfoSupplier) {
        this.srvUsername = srvUsername;
        this.abacClient = abacClient;
        this.auditLogger = auditLogger;
        this.subjectProvider = subjectProvider;
        this.auditRequestInfoSupplier = auditRequestInfoSupplier;
    }

    @Override
    public void sjekkTilgangTilEnhet(String veilederIdent, String enhetId) {
        ActionId actionId = ActionId.READ;
        Resource resource = lagEnhetResource(enhetId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.enhetIdMapper(enhetId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til enhet");
        }
    }

    @Override
    public void sjekkVeilederTilgangTilPerson(String veilederIdent, ActionId actionId, AbacPersonId personId) {
        Resource resource = lagPersonResource(personId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(personId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til bruker");
        }
    }

    @Override
    public void sjekkTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagPersonResource(personId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(personId, actionId, resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, subjectProvider.getSubjectFromToken(innloggetBrukerIdToken));

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Innlogget bruker har ikke tilgang til person");
        }
    }

    @Override
    public void sjekkTilgangTilOppfolging(String innloggetVeilederIdToken) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetVeilederIdToken);
        Resource resource = lagOppfolgingDomeneResource();
        String veilederIdent = subjectProvider.getSubjectFromToken(innloggetVeilederIdToken);

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                null,
                null,
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til oppfolging");
        }
    }

    @Override
    public void sjekkTilgangTilModia(String innloggetVeilederIdToken) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetVeilederIdToken);
        Resource resource = lagModiaDomeneResource();
        String veilederIdent = subjectProvider.getSubjectFromToken(innloggetVeilederIdToken);

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                null,
                null,
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til modia");
        }
    }

    @Override
    public void sjekkTilgangTilKode6(String veilederIdent) {
        Resource resource = lagKode6Resource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );
        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til kode 6");
        }
    }

    @Override
    public void sjekkTilgangTilKode7(String veilederIdent) {
        Resource resource = lagKode7Resource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

        if (!harTilgang(xacmlRequest, cefEventContext)) {
            throw new PepException("Veileder har ikke tilgang til kode 7");
        }
    }

    @Override
    public void sjekkTilgangTilEgenAnsatt(String veilederIdent) {
        Resource resource = lagEgenAnsattResource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
        CefAbacEventContext cefEventContext = lagCefEventContext(mapper, veilederIdent);

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

        auditLogger.logCef(xacmlRequest, xacmlResponse, cefEventContext);

        return XacmlResponseParser.harTilgang(xacmlResponse);
    }

    private CefAbacEventContext lagCefEventContext(CefAbacResponseMapper mapper, String subjectId) {
        Optional<AuditRequestInfo> requestInfo =
                Optional.ofNullable(auditRequestInfoSupplier).map(AuditRequestInfoSupplier::get);

        return CefAbacEventContext.builder()
                .applicationName(requireApplicationName())
                .callId(requestInfo.map(AuditRequestInfo::getCallId).orElse(null))
                .consumerId(requestInfo.map(AuditRequestInfo::getConsumerId).orElse(null))
                .requestMethod(requestInfo.map(AuditRequestInfo::getRequestMethod).orElse(null))
                .requestPath(requestInfo.map(AuditRequestInfo::getRequestPath).orElse(null))
                .subjectId(subjectId)
                .mapper(mapper)
                .build();
    }
}
