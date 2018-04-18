package no.nav.apiapp.config;

import lombok.SneakyThrows;
import no.nav.apiapp.ApiApplication;
import no.nav.brukerdialog.security.jaspic.OidcAuthModule;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CProvider;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.modig.core.context.ModigSecurityConstants;
import no.nav.modig.security.loginmodule.OpenAMLoginModule;
import no.nav.modig.security.loginmodule.SamlLoginModule;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.spi.LoginModule;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class Konfigurator implements ApiAppConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Konfigurator.class);

    public static final String OPENAM_RESTURL = "openam.restUrl";
    public static final String OPENAM_RESTURL_ENVIRONMENT_VARIABLE = "OPENAM_RESTURL";
    public static final String OPENAM_USER = ModigSecurityConstants.SYSTEMUSER_USERNAME;
    public static final String OPENAM_PASSWORD = ModigSecurityConstants.SYSTEMUSER_PASSWORD;
    public static final String AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME = "AAD_B2C_DISCOVERY_URL";
    public static final String AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME = "AAD_B2C_CLIENTID_USERNAME";

    private final Jetty.JettyBuilder jettyBuilder;
    private final ApiApplication apiApplication;
    private final List<OidcProvider> oidcProviders = new ArrayList<>();
    private boolean issoLogin;

    public Konfigurator(Jetty.JettyBuilder jettyBuilder, ApiApplication apiApplication) {
        this.jettyBuilder = jettyBuilder;
        this.apiApplication = apiApplication;
    }

    @Override
    public ApiAppConfigurator sts() {
        return sts(defaultStsConfig());
    }

    StsConfig defaultStsConfig() {
        return StsConfig.builder()
                .url(getConfigProperty(StsSecurityConstants.STS_URL_KEY, "SECURITYTOKENSERVICE_URL"))
                .username(getConfigProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, getSystemUserUsernamePropertyName()))
                .password(getConfigProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, getSystemUserPasswordPropertyName()))
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
        return openAmLogin(OpenAmConfig.builder()
                .restUrl(getConfigProperty(OPENAM_RESTURL, OPENAM_RESTURL_ENVIRONMENT_VARIABLE))
                .username(getConfigProperty(OPENAM_USER, getSystemUserUsernamePropertyName()))
                .password(getConfigProperty(OPENAM_PASSWORD, getSystemUserPasswordPropertyName()))
                .build()
        );
    }

    private String getSystemUserUsernamePropertyName() {
        return "SRV" + getAppName() + "_USERNAME";
    }

    private String getSystemUserPasswordPropertyName() {
        return "SRV" + getAppName() + "_PASSWORD";
    }

    @Override
    public ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig) {
        setupOpenAmLogin(jettyBuilder, openAmConfig);
        return this;
    }

    @Override
    public ApiAppConfigurator samlLogin() {
        return samlLogin(SamlConfig.builder().build());
    }

    @Override
    public ApiAppConfigurator issoLogin() {
        return issoLogin(IssoConfig.builder()
                .username(getConfigProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, getSystemUserUsernamePropertyName()))
                .password(getConfigProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, getSystemUserPasswordPropertyName()))
                .build());
    }

    @Override
    public ApiAppConfigurator issoLogin(IssoConfig issoConfig) {
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, issoConfig.username, PUBLIC);
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, issoConfig.password, SECRET);
        dialogArenaSubjectHandler();
        oidcProviders.add(new IssoOidcProvider());
        issoLogin = true;
        return this;
    }

    @Override
    public ApiAppConfigurator azureADB2CLogin() {
        return azureADB2CLogin(AzureADB2CConfig.builder()
                .discoveryUrl(getRequiredProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME))
                .expectedAudience(getRequiredProperty(AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME))
                .build()
        );
    }

    @Override
    public ApiAppConfigurator azureADB2CLogin(AzureADB2CConfig azureADB2CConfig) {
        dialogArenaSubjectHandler();
        oidcProviders.add(new AzureADB2CProvider(azureADB2CConfig));
        return this;
    }

    @Override
    public ApiAppConfigurator samlLogin(SamlConfig samlConfig) {
        modigSubjectHandler();
        dialogArenaSubjectHandler();
        LOGGER.info("configuring: {}", SamlLoginModule.class.getName());
        setLoginService(jettyBuilder, LoginModuleType.SAML);
        return this;
    }

    private String getConfigProperty(String primaryProperty, String secondaryProperty) {
        LOGGER.info("reading config-property {} / {}", primaryProperty, secondaryProperty);
        return getOptionalProperty(primaryProperty)
                .orElseGet(() -> getRequiredProperty(secondaryProperty));
    }

    private String getAppName() {
        return apiApplication.getApplicationName().toUpperCase();
    }

    public Jetty buildJetty() {
        if (!oidcProviders.isEmpty()) {
            jettyBuilder.configureForJaspic(new OidcAuthModule(oidcProviders, true), asList("/internal/*"));
        }
        return jettyBuilder.buildJetty();
    }

    @SneakyThrows
    public static void setupOpenAmLogin(Jetty.JettyBuilder jettyBuilder, OpenAmConfig openAmConfig) {
        setProperty(OPENAM_RESTURL, openAmConfig.restUrl, PUBLIC);
        setProperty(ModigSecurityConstants.SYSTEMUSER_USERNAME, openAmConfig.username, PUBLIC);
        setProperty(ModigSecurityConstants.SYSTEMUSER_PASSWORD, openAmConfig.password, SECRET);

        modigSubjectHandler();
        setLoginService(jettyBuilder, LoginModuleType.ESSO);
    }

    private static void setLoginService(Jetty.JettyBuilder jettyBuilder, LoginModuleType loginModuleType) {
        LOGGER.info("configuring: {}", loginModuleType.loginModuleClass);
        jettyBuilder.withLoginService(jaasLoginModule(loginModuleType));
    }

    private static JAASLoginService jaasLoginModule(LoginModuleType loginModuleType) {
        String jaasConfig = Konfigurator.class.getResource("/api-app/jaas.config").toExternalForm();
        setProperty("java.security.auth.login.config", jaasConfig, PUBLIC);
        JAASLoginService loginService = new JAASLoginService();
        loginService.setName(loginModuleType.moduleName);
        loginService.setLoginModuleName(loginModuleType.moduleName);
        loginService.setIdentityService(new DefaultIdentityService());
        return loginService;
    }

    private static void modigSubjectHandler() {
        setProperty(no.nav.apiapp.modigsecurity.JettySubjectHandler.SUBJECTHANDLER_KEY, no.nav.apiapp.modigsecurity.JettySubjectHandler.class.getName(), PUBLIC);
    }

    private static void dialogArenaSubjectHandler() {
        setProperty(no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY, no.nav.brukerdialog.security.context.JettySubjectHandler.class.getName(), PUBLIC);
    }

    public boolean harIssoLogin() {
        return issoLogin;
    }

    private enum LoginModuleType {
        ESSO("esso", OpenAMLoginModule.class),
        SAML("saml", SamlLoginModule.class);

        private final String moduleName;
        public Class<? extends LoginModule> loginModuleClass;

        LoginModuleType(String moduleName, Class<? extends LoginModule> loginModuleClass) {
            this.loginModuleClass = loginModuleClass;
            this.moduleName = moduleName;
        }

    }

}
