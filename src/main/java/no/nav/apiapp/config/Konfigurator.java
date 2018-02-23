package no.nav.apiapp.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.security.LoginConfigurator;
import no.nav.modig.core.context.ModigSecurityConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

public class Konfigurator implements ApiAppConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Konfigurator.class);

    public static final String OPENAM_RESTURL = "openam.restUrl";
    public static final String OPENAM_RESTURL_ENVIRONMENT_VARIABLE = "OPENAM_RESTURL";

    private final Jetty.JettyBuilder jettyBuilder;
    private final ApiApplication apiApplication;

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
                .username(getConfigProperty(ModigSecurityConstants.SYSTEMUSER_USERNAME, getSystemUserUsernamePropertyName()))
                .password(getConfigProperty(ModigSecurityConstants.SYSTEMUSER_PASSWORD, getSystemUserPasswordPropertyName()))
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
        LoginConfigurator.setupOpenAmLogin(jettyBuilder,openAmConfig);
        return this;
    }

    @Override
    public ApiAppConfigurator samlLogin() {
        return samlLogin(SamlConfig.builder().build());
    }

    @Override
    public ApiAppConfigurator samlLogin(SamlConfig samlConfig) {
        LoginConfigurator.setupSamlLogin(jettyBuilder);
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

}
