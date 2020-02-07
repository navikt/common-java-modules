package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.ApiApplication;
import no.nav.common.oidc.OidcTokenValidator;
import no.nav.common.oidc.auth.OidcAuthenticationFilter;
import no.nav.common.oidc.auth.OidcAuthenticator;
import no.nav.common.oidc.auth.OidcAuthenticatorConfig;
import no.nav.common.oidc.utils.TokenLocator;
import no.nav.json.JsonProvider;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import no.nav.sbl.dialogarena.types.Pingable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static no.nav.apiapp.rest.SwaggerResource.SWAGGER_JSON;
import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.*;

@Slf4j
public class Konfigurator implements ApiAppConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Konfigurator.class);

    private final JettyBuilder jettyBuilder;
    private final List<OidcAuthenticator> oidcAuthenticators = new ArrayList<>();
    private final List<Consumer<Jetty>> jettyCustomizers = new ArrayList<>();
    private final List<Consumer<JettyBuilder>> jettyBuilderCustomizers = new ArrayList<>();
    private final List<String> publicPaths = new ArrayList<>();
    private final List<Pingable> pingables = new ArrayList<>();

    private ObjectMapper objectMapper = JsonProvider.createObjectMapper();

    public Konfigurator(JettyBuilder jettyBuilder, ApiApplication apiApplication) {
        this.jettyBuilder = jettyBuilder;

        String apiBasePath = apiApplication.getApiBasePath();

        this.addPublicPath("/internal/.*")
            .addPublicPath("/ws/.*")
            .addPublicPath(joinPaths(apiBasePath, "/ping"))
            .addPublicPath(joinPaths(apiBasePath, SWAGGER_JSON));
    }

    @Override
    public ApiAppConfigurator sts() {
        return sts(defaultStsConfig());
    }

    StsConfig defaultStsConfig() {
        return StsConfig.builder()
                .url(getConfigProperty(StsSecurityConstants.STS_URL_KEY, "SECURITYTOKENSERVICE_URL"))
                .username(getConfigProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, resolveSrvUserPropertyName()))
                .password(getConfigProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, resolverSrvPasswordPropertyName()))
                .build();
    }

    @Override
    public ApiAppConfigurator sts(StsConfig stsConfig) {
        setProperty(StsSecurityConstants.STS_URL_KEY, stsConfig.url, PUBLIC);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, stsConfig.username, PUBLIC);
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, stsConfig.password, SECRET);
        return this;
    }

    @Override
    public ApiAppConfigurator addOidcAuthenticator(OidcAuthenticatorConfig config) {
        oidcAuthenticators.add(OidcAuthenticator.fromConfig(config));
        return this;
    }

    @Override
    public ApiAppConfigurator addPublicPath(String path) {
        publicPaths.add(path);
        return this;
    }

    @Override
    public ApiAppConfigurator customizeJetty(Consumer<Jetty> jettyCustomizer) {
        jettyCustomizers.add(jettyCustomizer);
        return this;
    }

    @Override
    public ApiAppConfigurator customizeJettyBuilder(Consumer<JettyBuilder> jettyBuilderCustomizer) {
        jettyBuilderCustomizers.add(jettyBuilderCustomizer);
        return this;
    }

    @Override
    public ApiAppConfigurator selfTest(Pingable pingable) {
        return selfTests(Collections.singletonList(pingable));
    }

    @Override
    public ApiAppConfigurator selfTests(Pingable... pingables) {
        return selfTests(Arrays.asList(pingables));
    }

    @Override
    public ApiAppConfigurator selfTests(Collection<? extends Pingable> pingables) {
        this.pingables.addAll(pingables);
        return this;
    }

    @Override
    public ApiAppConfigurator objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    private String getConfigProperty(String primaryProperty, String secondaryProperty) {
        LOGGER.info("reading config-property {} / {}", primaryProperty, secondaryProperty);
        return getOptionalProperty(primaryProperty)
                .orElseGet(() -> getRequiredProperty(secondaryProperty));
    }

    public Jetty buildJetty() {
        if (!oidcAuthenticators.isEmpty()) {
            jettyBuilder.addFilter(new OidcAuthenticationFilter(oidcAuthenticators, publicPaths));
        }

        jettyBuilderCustomizers.forEach(c -> c.accept(jettyBuilder));
        Jetty jetty = jettyBuilder.buildJetty();
        jettyCustomizers.forEach(c -> c.accept(jetty));
        return jetty;
    }

    public List<Pingable> getPingables() {
        return pingables;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
