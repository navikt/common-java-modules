package no.nav.common.abac;

import no.nav.common.abac.audit.*;
import no.nav.common.abac.cef.CefAbacEventContext;
import no.nav.common.abac.cef.CefAbacResponseMapper;
import no.nav.common.abac.constants.AbacDomain;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.abac.XacmlRequestBuilder.*;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

public class VeilarbPep implements Pep {

    private final AbacClient abacClient;

    private final String srvUsername;

    private final SubjectProvider subjectProvider;

    private final AuditConfig auditConfig;

    public VeilarbPep(String srvUsername,
                      AbacClient abacClient,
                      SubjectProvider subjectProvider,
                      AuditConfig auditConfig) {
        this.srvUsername = srvUsername;
        this.abacClient = abacClient;
        this.subjectProvider = subjectProvider;
        this.auditConfig = auditConfig;
    }

    @Override
    public boolean harVeilederTilgangTilEnhet(NavIdent veilederIdent, EnhetId enhetId) {
        ActionId actionId = ActionId.READ;
        Resource resource = lagEnhetResource(enhetId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.enhetIdMapper(enhetId, actionId, resource);
            return lagCefEventContext(mapper, veilederIdent.get());
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harTilgangTilEnhet(String innloggetBrukerIdToken, EnhetId enhetId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagEnhetResource(enhetId, AbacDomain.VEILARB_DOMAIN);
        ActionId actionId = ActionId.READ;

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                resource
        );
        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.enhetIdMapper(enhetId, actionId, resource);
            return lagCefEventContext(mapper, subjectProvider.getSubjectFromToken(innloggetBrukerIdToken));
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harTilgangTilEnhetMedSperre(String innloggetBrukerIdToken, EnhetId enhetId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagEnhetMedSperreResource(enhetId, AbacDomain.VEILARB_DOMAIN);
        ActionId actionId = ActionId.READ;

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.enhetIdMapper(enhetId, actionId, resource);
            return lagCefEventContext(mapper, subjectProvider.getSubjectFromToken(innloggetBrukerIdToken));
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harVeilederTilgangTilPerson(NavIdent veilederIdent, ActionId actionId, EksternBrukerId eksternBrukerId) {
        Resource resource = lagPersonResource(eksternBrukerId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                lagAction(actionId),
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(eksternBrukerId, actionId, resource);
            return lagCefEventContext(mapper, veilederIdent.get());
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, EksternBrukerId eksternBrukerId) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagPersonResource(eksternBrukerId, AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                lagAction(actionId),
                null,
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.personIdMapper(eksternBrukerId, actionId, resource);
            return lagCefEventContext(mapper, subjectProvider.getSubjectFromToken(innloggetBrukerIdToken));
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harTilgangTilOppfolging(String innloggetBrukerIdToken) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetBrukerIdToken);
        Resource resource = lagOppfolgingDomeneResource();
        String tokenSubject = subjectProvider.getSubjectFromToken(innloggetBrukerIdToken);

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                null,
                null,
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
            return lagCefEventContext(mapper, tokenSubject);
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harVeilederTilgangTilModia(String innloggetVeilederIdToken) {
        String oidcTokenBody = AbacUtils.extractOidcTokenBody(innloggetVeilederIdToken);
        Resource resource = lagModiaDomeneResource();
        String veilederIdent = subjectProvider.getSubjectFromToken(innloggetVeilederIdToken);

        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironmentMedOidcTokenBody(srvUsername, oidcTokenBody),
                null,
                null,
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
            return lagCefEventContext(mapper, veilederIdent);
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harVeilederTilgangTilKode6(NavIdent veilederIdent) {
        Resource resource = lagKode6Resource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
            return lagCefEventContext(mapper, veilederIdent.get());
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harVeilederTilgangTilKode7(NavIdent veilederIdent) {
        Resource resource = lagKode7Resource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
            return lagCefEventContext(mapper, veilederIdent.get());
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public boolean harVeilederTilgangTilEgenAnsatt(NavIdent veilederIdent) {
        Resource resource = lagEgenAnsattResource(AbacDomain.VEILARB_DOMAIN);
        XacmlRequest xacmlRequest = buildRequest(
                lagEnvironment(srvUsername),
                null,
                lagVeilederAccessSubject(veilederIdent),
                resource
        );

        Supplier<CefAbacEventContext> cefEventContext = () -> {
            CefAbacResponseMapper mapper = CefAbacResponseMapper.resourceMapper(resource);
            return lagCefEventContext(mapper, veilederIdent.get());
        };

        return harTilgang(xacmlRequest, cefEventContext);
    }

    @Override
    public AbacClient getAbacClient() {
        return abacClient;
    }

    private boolean harTilgang(XacmlRequest xacmlRequest,
                               Supplier<CefAbacEventContext> cefEventContext) {
        XacmlResponse xacmlResponse = abacClient.sendRequest(xacmlRequest);

        if (getAuditLogger() != null && cefEventContext != null && skalLogges(xacmlRequest, xacmlResponse)) {
            getAuditLogger().logCef(xacmlRequest, xacmlResponse, cefEventContext.get());
        }

        return XacmlResponseParser.harTilgang(xacmlResponse);
    }

    private boolean skalLogges(XacmlRequest xacmlRequest, XacmlResponse xacmlResponse) {
        return Optional
                .ofNullable(getAuditRequestInfoSupplier())
                .map(AuditRequestInfoSupplier::get)
                .map(auditRequestInfo -> Optional.ofNullable(getAuditLogFilter())
                        .map(filter -> filter.isEnabled(auditRequestInfo, xacmlRequest, xacmlResponse)).orElse(true))
                .orElse(false);
    }

    private AuditLogger getAuditLogger() {
        return auditConfig != null ? auditConfig.getAuditLogger() : null;
    }

    private AuditRequestInfoSupplier getAuditRequestInfoSupplier() {
        return auditConfig != null ? auditConfig.getAuditRequestInfoSupplier() : null;
    }

    private AuditLogFilter getAuditLogFilter() {
        return auditConfig != null ? auditConfig.getAuditLogFilter() : null;
    }

    private CefAbacEventContext lagCefEventContext(CefAbacResponseMapper mapper, String subjectId) {
        Optional<AuditRequestInfo> requestInfo =
                Optional.ofNullable(getAuditRequestInfoSupplier()).map(AuditRequestInfoSupplier::get);

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
