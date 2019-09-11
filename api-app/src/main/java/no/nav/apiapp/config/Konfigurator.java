package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.security.ApiAppAuthorizationModule;
import no.nav.apiapp.selftest.impl.OpenAMHelsesjekk;
import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.security.jaspic.OidcAuthModule;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProviderConfig;
import no.nav.brukerdialog.security.oidc.provider.*;
import no.nav.brukerdialog.security.pingable.IssoIsAliveHelsesjekk;
import no.nav.brukerdialog.security.pingable.IssoSystemBrukerTokenHelsesjekk;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.LoginFilter;
import no.nav.common.auth.LoginProvider;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.openam.sbs.OpenAMLoginFilter;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
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
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.configureAzureAdForExternalUsers;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.configureAzureAdForInternalUsers;
import static no.nav.util.sbl.EnvironmentUtils.Type.PUBLIC;
import static no.nav.util.sbl.EnvironmentUtils.Type.SECRET;
import static no.nav.util.sbl.EnvironmentUtils.*;

@Slf4j
public class Konfigurator implements ApiAppConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Konfigurator.class);

    private final JettyBuilder jettyBuilder;
    private final List<OidcProvider> oidcProviders = new ArrayList<>();
    private final List<LoginProvider> loginProviders = new ArrayList<>();
    private final List<Consumer<Jetty>> jettyCustomizers = new ArrayList<>();
    private final List<Consumer<JettyBuilder>> jettyBuilderCustomizers = new ArrayList<>();
    private final List<String> publicPaths = new ArrayList<>();
    private final List<Object> springBonner = new ArrayList<>();
    private final List<Pingable> pingables = new ArrayList<>();

    private AuthorizationModule authorizationModule;
    private ObjectMapper objectMapper = JsonProvider.createObjectMapper();
    private SecurityLevel defaultSecurityLevel = null;
    private Map<SecurityLevel, List<String>> securityLevelForBasePaths = new HashMap<>();

    public Konfigurator(JettyBuilder jettyBuilder, ApiApplication apiApplication) {
        this.jettyBuilder = jettyBuilder;

        String apiBasePath = apiApplication.getApiBasePath();
        this
                .addPublicPath("/internal/.*")
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
    public ApiAppConfigurator openAmLogin() {
        return openAmLogin(OpenAmConfig.fromSystemProperties());
    }

    @Override
    public ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig) {
        loginProviders.add(new OpenAMLoginFilter(openAmConfig));
        springBonner.add(new OpenAMHelsesjekk(openAmConfig));
        return this;
    }

    @Override
    public ApiAppConfigurator issoLogin() {
        return issoLogin(IssoConfig.builder()
                .username(getConfigProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, resolveSrvUserPropertyName()))
                .password(getConfigProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, resolverSrvPasswordPropertyName()))
                .issoHostUrl(Constants.getIssoHostUrl())
                .issoRpUserUsername(Constants.getIssoRpUserUsername())
                .issoRpUserPassword(Constants.getIssoRpUserPassword())
                .issoJwksUrl(Constants.getIssoJwksUrl())
                .issoExpectedTokenIssuer(Constants.getIssoExpectedTokenIssuer())
                .oidcRedirectUrl(Constants.getOidcRedirectUrl())
                .isAliveUrl(Constants.getIssoIsaliveUrl())
                .build()
        );
    }

    @Override
    public ApiAppConfigurator issoLogin(IssoConfig issoConfig) {
        SystemUserTokenProviderConfig systemUserTokenProviderConfig = SystemUserTokenProviderConfig.builder()
                .srvUsername(issoConfig.username)
                .srvPassword(issoConfig.password)
                .issoHostUrl(issoConfig.issoHostUrl)
                .issoRpUserUsername(issoConfig.issoRpUserUsername)
                .issoRpUserPassword(issoConfig.issoRpUserPassword)
                .issoJwksUrl(issoConfig.issoJwksUrl)
                .issoExpectedTokenIssuer(issoConfig.issoExpectedTokenIssuer)
                .oidcRedirectUrl(issoConfig.oidcRedirectUrl)
                .build();

        SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider(systemUserTokenProviderConfig);
        springBonner.add(systemUserTokenProvider);
        springBonner.add(new IssoSystemBrukerTokenHelsesjekk(systemUserTokenProvider));
        springBonner.add(new IssoIsAliveHelsesjekk(issoConfig.isAliveUrl));
        return oidcProvider(new IssoOidcProvider(IssoOidcProviderConfig.from(systemUserTokenProviderConfig)));
    }

    @Override
    @Deprecated
    public ApiAppConfigurator azureADB2CLogin() {
        return azureADB2CLogin(configureAzureAdForExternalUsers());
    }

    @Override
    @Deprecated
    public ApiAppConfigurator azureADB2CLogin(AzureADB2CConfig azureADB2CConfig) {
        return oidcProvider(new AzureADB2CProvider(azureADB2CConfig));
    }


    @Override
    public ApiAppConfigurator validateAzureAdExternalUserTokens() {
        return oidcProvider(new AzureADB2CProvider(configureAzureAdForExternalUsers()));
    }

    @Override
    public ApiAppConfigurator validateAzureAdExternalUserTokens(SecurityLevel defaultSecurityLevel) {
        this.defaultSecurityLevel = defaultSecurityLevel;
        return validateAzureAdExternalUserTokens();
    }

    @Override
    public ApiAppConfigurator customSecurityLevelForExternalUsers(SecurityLevel securityLevel, String... basePath) {
        this.securityLevelForBasePaths.put(securityLevel, Arrays.asList(basePath));
        return this;
    }

    @Override
    public ApiAppConfigurator validateAzureAdInternalUsersTokens() {
        return oidcProvider(new InternalUserLoginProvider(configureAzureAdForInternalUsers()));
    }

    @Override
    public ApiAppConfigurator securityTokenServiceLogin() {
        return securityTokenServiceLogin(SecurityTokenServiceOidcProviderConfig.readFromSystemProperties());
    }

    @Override
    public ApiAppConfigurator securityTokenServiceLogin(SecurityTokenServiceOidcProviderConfig securityTokenServiceOidcProviderConfig) {
        return oidcProvider(new SecurityTokenServiceOidcProvider(securityTokenServiceOidcProviderConfig));
    }

    @Override
    public ApiAppConfigurator oidcProvider(OidcProvider oidcProvider) {
        oidcProviders.add(oidcProvider);
        springBonner.add(oidcProvider);
        return this;
    }

    @Override
    public ApiAppConfigurator addPublicPath(String path) {
        publicPaths.add(path);
        return this;
    }

    @Override
    public ApiAppConfigurator authorizationModule(AuthorizationModule authorizationModule) {
        this.authorizationModule = authorizationModule;
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
        if (!oidcProviders.isEmpty()) {
            loginProviders.add(new OidcAuthModule(oidcProviders));
        }
        if (hasLogin()) {
            log.info("adding {} with loginProviders={} authorizationModule={} publicPaths={}",
                    LoginFilter.class.getSimpleName(),
                    loginProviders,
                    authorizationModule,
                    publicPaths
            );
            jettyBuilder.addFilter(new LoginFilter(
                    loginProviders,
                    new ApiAppAuthorizationModule(authorizationModule, defaultSecurityLevel, securityLevelForBasePaths),
                    publicPaths));
        }
        jettyBuilderCustomizers.forEach(c -> c.accept(jettyBuilder));
        Jetty jetty = jettyBuilder.buildJetty();
        jettyCustomizers.forEach(c -> c.accept(jetty));
        return jetty;
    }

    public boolean hasLogin() {
        return !oidcProviders.isEmpty() || !loginProviders.isEmpty() || authorizationModule != null;
    }

    public List<Object> getSpringBonner() {
        return springBonner;
    }

    public List<Pingable> getPingables() {
        return pingables;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
