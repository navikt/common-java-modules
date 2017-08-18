package no.nav.dialogarena.config;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.context.TestSubjectHandler;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;
import no.nav.dialogarena.config.fasit.OpenAmConfig;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.security.ESSOProvider;
import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;
import no.nav.modig.security.loginmodule.OpenAMLoginModule;
import no.nav.modig.security.loginmodule.SamlLoginModule;
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
import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE;
import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE_Q;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.disableCertificateChecks;
import static org.apache.commons.io.IOUtils.write;
import static org.slf4j.LoggerFactory.getLogger;

public class DevelopmentSecurity {

    private static final Logger LOG = getLogger(DevelopmentSecurity.class);

    public static final String DEFAULT_ISSO_RP_USER = "isso-rp-user";
    public static final String DEFAULT_LDAP_USER = "ldap";

    @Setter
    @Accessors(chain = true)
    public static class ISSOSecurityConfig {

        private final String applicationName;
        private final String environment;

        private String issoUserName;
        private String serviceUserName;
        private String contextName;
        private String ldapUserAlias;

        public ISSOSecurityConfig(String applicationName, String environment) {
            this.applicationName = applicationName;
            this.environment = environment;

            this.contextName = applicationName;
            this.issoUserName = DEFAULT_ISSO_RP_USER;
            this.serviceUserName = "srv" + applicationName;
            this.ldapUserAlias = DEFAULT_LDAP_USER;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class ESSOSecurityConfig {

        private final String applicationName;
        private final String environment;

        private String serviceUserName;

        public ESSOSecurityConfig(String applicationName, String environment) {
            this.applicationName = applicationName;
            this.environment = environment;
            this.serviceUserName = "srv" + applicationName;
        }
    }

    @Setter
    @Accessors(chain = true)
    public static class SamlSecurityConfig {

        private final String applicationName;
        private final String environment;

        private String serviceUserName;
        private String ldapUserAlias;

        public SamlSecurityConfig(String applicationName, String environment) {
            this.applicationName = applicationName;
            this.environment = environment;
            this.serviceUserName = "srv" + applicationName;

            this.ldapUserAlias = DEFAULT_LDAP_USER;
        }
    }


    @SneakyThrows
    public static Jetty.JettyBuilder setupSamlLogin(Jetty.JettyBuilder jettyBuilder, SamlSecurityConfig securityConfig) {
        commonServerSetup(jettyBuilder);

        String environment = securityConfig.environment;
        ServiceUser serviceUser = FasitUtils.getServiceUser(securityConfig.serviceUserName, securityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, FasitUtils.getFSSLocal(environment));
        configureServiceUser(serviceUser);
        configureAbacUser(serviceUser);
        configureLdap(getLdapConfig(securityConfig.ldapUserAlias, securityConfig.applicationName, securityConfig.environment));
        modigSubjectHandler();
        dialogArenaSubjectHandler();

        LOG.info("configuring: {}", SamlLoginModule.class.getName());
        return jettyBuilder.withLoginService(jaasLoginModule(SAML));
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupESSO(Jetty.JettyBuilder jettyBuilder, ESSOSecurityConfig essoSecurityConfig) {
        commonServerSetup(jettyBuilder);
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

        String environment = issoSecurityConfig.environment;

        configureIsso(FasitUtils.getServiceUser(issoSecurityConfig.issoUserName, issoSecurityConfig.applicationName, environment));

        ServiceUser serviceUser = FasitUtils.getServiceUser(issoSecurityConfig.serviceUserName, issoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, FasitUtils.getFSSLocal(environment));
        configureServiceUser(serviceUser);
        configureAbacUser(serviceUser);
        configureLdap(getLdapConfig(issoSecurityConfig.ldapUserAlias, issoSecurityConfig.applicationName, issoSecurityConfig.environment));

        modigSubjectHandler();
        dialogArenaSubjectHandler();
        return configureJaspi(jettyBuilder, issoSecurityConfig.contextName);
    }

    private static void configureIsso(ServiceUser issoCredentials) {
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, issoCredentials.username);
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, issoCredentials.password);
        String environmentClass = getEnvironmentClass(issoCredentials.environment);
        setProperty(ISSO_JWKS_URL, format("https://isso-%s.adeo.no/isso/oauth2/connect/jwk_uri", environmentClass));
        setProperty(ISSO_EXPECTED_TOKEN_ISSUER, format("https://isso-%s.adeo.no:443/isso/oauth2", environmentClass)); // OBS OBS, må sette port 443 her av en eller annen merkelig grunn!
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, format("https://isso-%s.adeo.no/isso/oauth2", environmentClass));
        setProperty("isso.isalive.url", format("https://isso-%s.adeo.no/isso/isAlive.jsp", environmentClass));
        setProperty(OIDC_REDIRECT_URL, getRedirectUrl(issoCredentials.environment));
    }

    private static String getRedirectUrl(String environment) {
        switch (getEnvironmentClass(environment)) {
            case "t":
                return KJENT_LOGIN_ADRESSE;
            case "q":
                return KJENT_LOGIN_ADRESSE_Q;
            default:
                throw new IllegalStateException();
        }
    }

    private static void configureAbacUser(ServiceUser serviceUser) {
        setProperty("no.nav.abac.systemuser.username", serviceUser.username);
        setProperty("no.nav.abac.systemuser.password", serviceUser.password);
    }

    public static void configureLdap(LdapConfig ldapConfig) {
        setProperty("abac.endpoint.url", String.format("https://wasapp-%s.adeo.no/asm-pdp/authorize", ldapConfig.environment));
        setProperty("ldap.username", ldapConfig.username);
        setProperty("ldap.password", ldapConfig.password);
        setProperty("ldap.url", "ldaps://ldapgw.test.local");
        setProperty("ldap.basedn", "dc=test,dc=local");
        setProperty("role", "0000-GA-Modia-Oppfolging");
    }

    public static void setupIntegrationTestSecurity(ServiceUser serviceUser) {
        commonSetup();

        Subject testSubject = integrationTestSubject(serviceUser);
        configureServiceUser(serviceUser);
        if (!erEksterntDomene(serviceUser.getDomain())) {
            // Her jukser vi litt, denne brukeren er nok ikke gyldig
            configureAbacUser(serviceUser);
            configureIsso(serviceUser);
            //

            dialogArenaSubjectHandler(InternbrukerSubjectHandler.class);
            InternbrukerSubjectHandler internbrukerSubjectHandler = (InternbrukerSubjectHandler) TestSubjectHandler.getSubjectHandler();
            internbrukerSubjectHandler.setSubject(testSubject);
        }
        modigSubjectHandler(StaticSubjectHandler.class);
        StaticSubjectHandler staticSubjectHandler = (StaticSubjectHandler) StaticSubjectHandler.getSubjectHandler();
        staticSubjectHandler.setSubject(testSubject);
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
