package no.nav.dialogarena.config;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.context.TestSubjectHandler;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.OpenAmConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.security.ESSOProvider;
import no.nav.dialogarena.config.util.Util;
import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;
import no.nav.modig.security.loginmodule.OpenAMLoginModule;
import no.nav.modig.security.loginmodule.SamlLoginModule;
import no.nav.modig.testcertificates.TestCertificates;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.apache.commons.io.IOUtils;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.String.format;
import static javax.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.ESSO;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.SAML;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.security.ISSOProvider.LOGIN_APPLIKASJON;
import static no.nav.dialogarena.config.util.Util.Mode.IKKE_OVERSKRIV;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.disableCertificateChecks;
import static org.apache.commons.io.IOUtils.write;
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

        public IntegrationTestConfig(String applicationName) {
            this.applicationName = applicationName;

            this.environment = getDefaultEnvironment();
            this.serviceUserName = "srv" + applicationName;
            this.issoUserName = DEFAULT_ISSO_RP_USER;
        }
    }


    @SneakyThrows
    public static Jetty.JettyBuilder setupSamlLogin(Jetty.JettyBuilder jettyBuilder, SamlSecurityConfig securityConfig) {
        commonServerSetup(jettyBuilder);
        appSetup(securityConfig.applicationName,securityConfig.environment);
        modigSubjectHandler();
        dialogArenaSubjectHandler();
        LOG.info("configuring: {}", SamlLoginModule.class.getName());
        return jettyBuilder.withLoginService(jaasLoginModule(SAML));
    }

    @SneakyThrows
    public static void setupESSO(ESSOSecurityConfig essoSecurityConfig) {
        commonSetup();
        appSetup(essoSecurityConfig.applicationName, essoSecurityConfig.environment);
        modigSubjectHandler();
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupESSO(Jetty.JettyBuilder jettyBuilder, ESSOSecurityConfig essoSecurityConfig) {
        commonServerSetup(jettyBuilder);
        appSetup(essoSecurityConfig.applicationName, essoSecurityConfig.environment);
        String environment = essoSecurityConfig.environment;

        ServiceUser serviceUser = FasitUtils.getServiceUser(essoSecurityConfig.serviceUserName, essoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, FasitUtils.getOeraLocal(environment));
        configureServiceUser(serviceUser);
        modigSubjectHandler();

        return configureOpenAm(jettyBuilder, essoSecurityConfig);
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupISSO(Jetty.JettyBuilder jettyBuilder, ISSOSecurityConfig issoSecurityConfig) {
        commonServerSetup(jettyBuilder);
        appSetup(issoSecurityConfig.applicationName, issoSecurityConfig.environment);
        modigSubjectHandler();
        dialogArenaSubjectHandler();
        return configureJaspi(jettyBuilder, issoSecurityConfig.contextName);
    }

    @SneakyThrows
    public static void setupISSO(ISSOSecurityConfig issoSecurityConfig) {
        commonSetup();
        appSetup(issoSecurityConfig.applicationName,issoSecurityConfig.environment);
        modigSubjectHandler();
        dialogArenaSubjectHandler();
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
        commonSetup();
        appSetup(integrationTestConfig.applicationName,integrationTestConfig.environment);

        ServiceUser serviceUser = FasitUtils.getServiceUser(integrationTestConfig.serviceUserName, integrationTestConfig.applicationName, integrationTestConfig.environment);
        Subject testSubject = integrationTestSubject(serviceUser);
        configureServiceUser(serviceUser);
        if (!erEksterntDomene(serviceUser.getDomain())) {
            dialogArenaSubjectHandler(InternbrukerSubjectHandler.class);
            InternbrukerSubjectHandler internbrukerSubjectHandler = (InternbrukerSubjectHandler) TestSubjectHandler.getSubjectHandler();
            internbrukerSubjectHandler.setSubject(testSubject);
        }
        modigSubjectHandler(StaticSubjectHandler.class);
        StaticSubjectHandler staticSubjectHandler = (StaticSubjectHandler) StaticSubjectHandler.getSubjectHandler();
        staticSubjectHandler.setSubject(testSubject);
    }

    public static void appSetup(String applicationName, String environment) {
        Util.setProperties(FasitUtils.getApplicationEnvironment(applicationName, environment), IKKE_OVERSKRIV);
        // reset keystore siden dette kan ha blitt endret
        TestCertificates.setupKeyAndTrustStore();
    }

    private static Subject integrationTestSubject(ServiceUser serviceUser) {
        ESSOProvider.ESSOCredentials essoCredentials = ESSOProvider.getEssoCredentials(serviceUser.environment);
        String sso = essoCredentials.sso;
        String username = essoCredentials.testUser.username;

        Subject subject = new Subject();
        subject.getPrincipals().add(SluttBruker.eksternBruker(username));
        subject.getPrincipals().add(new ConsumerId(username));
        subject.getPublicCredentials().add(new OpenAmTokenCredential(sso));
        subject.getPublicCredentials().add(new AuthenticationLevelCredential(4));
        return subject;
    }

    private static void commonServerSetup(Jetty.JettyBuilder jettyBuilder) {
        commonSetup();
        jettyBuilder.disableAnnotationScanning();
    }

    private static void commonSetup() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", "app");
        disableCertificateChecks();

        // selve keystore/truststore bør være unødvendig siden vi disabler serifikatsjekker,
        // men det settes også system-properties som vi ofte må ha i test/utvikling
        setupKeyAndTrustStore();

        setProperty("environment.class", "t");
        setProperty("disable.metrics.report", Boolean.TRUE.toString());
        setProperty("jetty.print.localhost", Boolean.TRUE.toString());
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
        setProperty("openam.restUrl", openAmConfig.restUrl);
        setProperty("openam.logoutUrl", openAmConfig.logoutUrl);
        LOG.info("configuring: {}", OpenAMLoginModule.class.getName());
        return jettyBuilder.withLoginService(jaasLoginModule(ESSO));
    }

    public static JAASLoginService jaasLoginModule(LoginModuleType loginModuleType) {
        setProperty("java.security.auth.login.config", DevelopmentSecurity.class.getResource("/jaas.config").toExternalForm());
        JAASLoginService loginService = new JAASLoginService();
        String moduleName = loginModuleType.getModuleName();
        loginService.setName(moduleName);
        loginService.setLoginModuleName(moduleName);
        loginService.setIdentityService(new DefaultIdentityService());
        return loginService;
    }

    private static void modigSubjectHandler() {
        modigSubjectHandler(no.nav.modig.core.context.JettySubjectHandler.class);
    }

    private static void modigSubjectHandler(Class<? extends no.nav.modig.core.context.SubjectHandler> jettySubjectHandlerClass) {
        setProperty(no.nav.modig.core.context.JettySubjectHandler.SUBJECTHANDLER_KEY, jettySubjectHandlerClass.getName());
    }

    private static void dialogArenaSubjectHandler() {
        dialogArenaSubjectHandler(no.nav.brukerdialog.security.context.JettySubjectHandler.class);
    }

    private static void dialogArenaSubjectHandler(Class<? extends no.nav.brukerdialog.security.context.SubjectHandler> jettySubjectHandlerClass) {
        setProperty(no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY, jettySubjectHandlerClass.getName());
    }

    private static Jetty.JettyBuilder configureJaspi(Jetty.JettyBuilder jettyBuilder, String contextName) {
        File target = new File("target");
        target.mkdirs();
        File jaspiConfigFile = new File(target, "jaspiconf.xml");
        try (FileOutputStream fileOutputStream = new FileOutputStream(jaspiConfigFile)) {
            write(readJaspiConfig(contextName), fileOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String absolutePath = jaspiConfigFile.getAbsolutePath();
        LOG.info("jaspi contiguration at: {}", absolutePath);
        setProperty("org.apache.geronimo.jaspic.configurationFile", absolutePath);
        // NB hvis man kjører på appserver med annotasjons-scanning, blir denne oppdaget automatisk
        setProperty(DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
        return jettyBuilder.configureForJaspic();
    }

    private static String readJaspiConfig(String contextName) throws IOException {
        return IOUtils.toString(DevelopmentSecurity.class.getResourceAsStream("/jaspiconf.xml"))
                .replace("%%CONTEXT_PATH%%", contextName);
    }

    public enum LoginModuleType {
        ESSO("esso"),
        SAML("saml");

        private final String moduleName;

        LoginModuleType(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getModuleName() {
            return moduleName;
        }

    }
}
