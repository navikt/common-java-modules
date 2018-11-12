package no.nav.dialogarena.config;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.common.auth.LoginFilter;
import no.nav.common.auth.LoginProvider;
import no.nav.common.auth.openam.sbs.OpenAMLoginFilter;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.OpenAmConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.util.Util;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.testconfig.ApiAppTest;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDefaultEnvironment;
import static no.nav.dialogarena.config.security.ISSOProvider.LOGIN_APPLIKASJON;
import static no.nav.dialogarena.config.util.Util.Mode.IKKE_OVERSKRIV;
import static no.nav.dialogarena.config.util.Util.Mode.OVERSKRIV;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.common.jetty.ToUrl.JETTY_PRINT_LOCALHOST;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.disableCertificateChecks;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.setupKeyAndTrustStore;
import static no.nav.sbl.util.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.slf4j.LoggerFactory.getLogger;

public class DevelopmentSecurity {

    private static final Logger LOG = getLogger(DevelopmentSecurity.class);

    public static final String DEFAULT_ISSO_RP_USER = "isso-rp-user";

    @Setter
    @Accessors(chain = true)
    public static class ISSOSecurityConfig {

        private final String applicationName;

        private String environment;
        private String issoUserName;
        private String serviceUserName;
        private String contextName;

        public ISSOSecurityConfig(String applicationName) {
            this.applicationName = applicationName;

            this.environment = getDefaultEnvironment();
            this.contextName = applicationName;
            this.issoUserName = DEFAULT_ISSO_RP_USER;
            this.serviceUserName = "srv" + applicationName;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class ESSOSecurityConfig {

        private final String applicationName;

        private String environment;
        private String serviceUserName;

        public ESSOSecurityConfig(String applicationName) {
            this.applicationName = applicationName;

            this.environment = getDefaultEnvironment();
            this.serviceUserName = "srv" + applicationName;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class SamlSecurityConfig {

        private final String applicationName;

        private String environment;
        private String serviceUserName;

        public SamlSecurityConfig(String applicationName) {
            this.applicationName = applicationName;

            this.environment = getDefaultEnvironment();
            this.serviceUserName = "srv" + applicationName;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class IntegrationTestConfig {

        private final String applicationName;
        private String environment;

        private String serviceUserName;
        private String issoUserName;
        private boolean overskrivSystemProperties = true;

        public IntegrationTestConfig(String applicationName) {
            this.applicationName = applicationName;

            this.environment = getDefaultEnvironment();
            this.serviceUserName = "srv" + applicationName;
            this.issoUserName = DEFAULT_ISSO_RP_USER;
        }
    }


    @SneakyThrows
    public static Jetty.JettyBuilder setupSamlLogin(Jetty.JettyBuilder jettyBuilder, SamlSecurityConfig securityConfig) {
        commonServerSetup(jettyBuilder,securityConfig.applicationName);
        appSetup(securityConfig.applicationName,securityConfig.environment);
        return jettyBuilder;
    }

    @SneakyThrows
    public static void setupESSO(ESSOSecurityConfig essoSecurityConfig) {
        commonSetup(essoSecurityConfig.applicationName);
        appSetup(essoSecurityConfig.applicationName, essoSecurityConfig.environment);
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupESSO(Jetty.JettyBuilder jettyBuilder, ESSOSecurityConfig essoSecurityConfig) {
        commonServerSetup(jettyBuilder,essoSecurityConfig.applicationName);
        appSetup(essoSecurityConfig.applicationName, essoSecurityConfig.environment);
        String environment = essoSecurityConfig.environment;

        ServiceUser serviceUser = FasitUtils.getServiceUser(essoSecurityConfig.serviceUserName, essoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, FasitUtils.getOeraLocal(environment));
        configureServiceUser(serviceUser);

        return configureOpenAm(jettyBuilder, essoSecurityConfig);
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupISSO(Jetty.JettyBuilder jettyBuilder, ISSOSecurityConfig issoSecurityConfig) {
        commonServerSetup(jettyBuilder,issoSecurityConfig.applicationName);
        appSetup(issoSecurityConfig.applicationName, issoSecurityConfig.environment);
        return jettyBuilder;
    }

    @SneakyThrows
    public static void setupISSO(ISSOSecurityConfig issoSecurityConfig) {
        commonSetup(issoSecurityConfig.applicationName);
        appSetup(issoSecurityConfig.applicationName,issoSecurityConfig.environment);
    }

    @Deprecated
    public static void setupIntegrationTestSecurity(ServiceUser serviceUser) {
        String applicationName = serviceUser.username.substring(3); // fjern 'srv'
        setupIntegrationTestSecurity(new IntegrationTestConfig(applicationName).setEnvironment(serviceUser.environment));
    }

    public static String getRedirectUrl(String environment) {
        return String.format("https://app-%s.adeo.no/%s/api/login", environment, LOGIN_APPLIKASJON);
    }


    public static void setupIntegrationTestSecurity(IntegrationTestConfig integrationTestConfig) {
        commonSetup(integrationTestConfig.applicationName);
        appSetup(
                integrationTestConfig.applicationName,
                integrationTestConfig.environment,
                integrationTestConfig.overskrivSystemProperties ? OVERSKRIV : IKKE_OVERSKRIV
        );

        ServiceUser serviceUser = FasitUtils.getServiceUser(integrationTestConfig.serviceUserName, integrationTestConfig.applicationName, integrationTestConfig.environment);
        configureServiceUser(serviceUser);
    }

    public static void appSetup(String applicationName, String environment) {
        appSetup(applicationName, environment, IKKE_OVERSKRIV);
    }

    private static void appSetup(String applicationName, String environment, Util.Mode mode) {
        Util.setProperties(FasitUtils.getApplicationEnvironment(applicationName, environment), mode);
        // reset keystore siden dette kan ha blitt endret
        setupKeyAndTrustStore();
    }

    private static void commonServerSetup(Jetty.JettyBuilder jettyBuilder, String applicationName) {
        commonSetup(applicationName);
        jettyBuilder.disableAnnotationScanning();
    }

    private static void commonSetup(String applicationName) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(applicationName)
                .build());
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", "app");
        disableCertificateChecks();

        // selve keystore/truststore bør være unødvendig siden vi disabler serifikatsjekker,
        // men det settes også system-properties som vi ofte må ha i test/utvikling
        setupKeyAndTrustStore();

        setProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, "t");
        setProperty(JETTY_PRINT_LOCALHOST, Boolean.TRUE.toString());
        setProperty(AnnotationConfiguration.MAX_SCAN_WAIT, "120");
    }

    private static void assertCorrectDomain(ServiceUser serviceUser, String expectedDomain) {
        if (!expectedDomain.equals(serviceUser.domain)) {
            throw new IllegalStateException(String.format("%s != %s", serviceUser.domain, expectedDomain));
        }
    }

    private static void configureServiceUser(ServiceUser serviceUser) {
        setProperty(SYSTEMUSER_USERNAME, serviceUser.username);
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.password);
        setProperty(STS_URL_KEY, format("https://sts-%s.%s/SecurityTokenServiceProvider/", serviceUser.environment, serviceUser.domain));
    }

    private static Jetty.JettyBuilder configureOpenAm(Jetty.JettyBuilder jettyBuilder, ESSOSecurityConfig essoSecurityConfig) throws IOException {
        OpenAmConfig openAmConfig = FasitUtils.getOpenAmConfig(essoSecurityConfig.environment);
        List<LoginProvider> loginProviders = singletonList(new OpenAMLoginFilter(no.nav.common.auth.openam.sbs.OpenAmConfig.builder().restUrl(openAmConfig.restUrl).build()));
        return jettyBuilder.addFilter(new LoginFilter(loginProviders, singletonList(".*")));
    }

}
