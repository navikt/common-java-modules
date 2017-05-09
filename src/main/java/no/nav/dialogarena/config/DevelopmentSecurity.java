package no.nav.dialogarena.config;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.context.SubjectHandler;
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
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.ESSO;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.SAML;
import static no.nav.dialogarena.config.fasit.FasitUtils.OERA_T_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.TEST_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.getLdapConfig;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static org.apache.commons.io.IOUtils.write;
import static org.slf4j.LoggerFactory.getLogger;

public class DevelopmentSecurity {

    private static final Logger LOG = getLogger(DevelopmentSecurity.class);

    private static final Class<no.nav.modig.core.context.JettySubjectHandler> MODIG_SUBJECT_HANDLER_CLASS = no.nav.modig.core.context.JettySubjectHandler.class;
    private static final Class<no.nav.brukerdialog.security.context.JettySubjectHandler> OIDC_SUBJECT_HANDLER_CLASS = no.nav.brukerdialog.security.context.JettySubjectHandler.class;

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

        public SamlSecurityConfig(String applicationName, String environment) {
            this.applicationName = applicationName;
            this.environment = environment;
            this.serviceUserName = "srv" + applicationName;
        }
    }


    @SneakyThrows
    public static Jetty.JettyBuilder setupSamlLogin(Jetty.JettyBuilder jettyBuilder, SamlSecurityConfig essoSecurityConfig) {
        commonServerSetup(jettyBuilder);

        String environment = essoSecurityConfig.environment;
        ServiceUser serviceUser = FasitUtils.getServiceUser(essoSecurityConfig.serviceUserName, essoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, TEST_LOCAL);
        configureServiceUser(serviceUser);
        configureSubjectHandler(MODIG_SUBJECT_HANDLER_CLASS);

        LOG.info("configuring: {}", SamlLoginModule.class.getName());
        return jettyBuilder.withLoginService(jaasLoginModule(SAML));
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupESSO(Jetty.JettyBuilder jettyBuilder, ESSOSecurityConfig essoSecurityConfig) {
        commonServerSetup(jettyBuilder);
        String environment = essoSecurityConfig.environment;

        ServiceUser serviceUser = FasitUtils.getServiceUser(essoSecurityConfig.serviceUserName, essoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, OERA_T_LOCAL);
        configureServiceUser(serviceUser);
        configureSubjectHandler(MODIG_SUBJECT_HANDLER_CLASS);
        return configureOpenAm(jettyBuilder, essoSecurityConfig);
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupISSO(Jetty.JettyBuilder jettyBuilder, ISSOSecurityConfig issoSecurityConfig) {
        commonServerSetup(jettyBuilder);

        String environment = issoSecurityConfig.environment;
        String environmentShort = environment.substring(0, 1);

        ServiceUser issoCredentials = FasitUtils.getServiceUser(issoSecurityConfig.issoUserName, issoSecurityConfig.applicationName, environment);
        setProperty("isso-rp-user.username", issoCredentials.username);
        setProperty("isso-rp-user.password", issoCredentials.password);
        setProperty("isso-jwks.url", format("https://isso-%s.adeo.no/isso/oauth2/connect/jwk_uri", environmentShort));
        setProperty("isso-issuer.url", format("https://isso-%s.adeo.no:443/isso/oauth2", environmentShort)); // OBS OBS, må sette port 443 her av en eller annen merkelig grunn!
        setProperty("isso-host.url", format("https://isso-%s.adeo.no/isso/oauth2", environmentShort));
        setProperty("oidc-redirect.url", format("/%s/tjenester/login", issoSecurityConfig.contextName));

        ServiceUser serviceUser = FasitUtils.getServiceUser(issoSecurityConfig.serviceUserName, issoSecurityConfig.applicationName, environment);
        assertCorrectDomain(serviceUser, TEST_LOCAL);
        configureServiceUser(serviceUser);
        configureAbacUser(serviceUser);
        configureLdap(getLdapConfig(issoSecurityConfig.ldapUserAlias, issoSecurityConfig.applicationName, issoSecurityConfig.environment));

        configureSubjectHandler(OIDC_SUBJECT_HANDLER_CLASS);
        return configureJaspi(jettyBuilder, issoSecurityConfig.contextName);
    }

    private static void configureAbacUser(ServiceUser serviceUser) {
        setProperty("no.nav.abac.systemuser.username", serviceUser.username);
        setProperty("no.nav.abac.systemuser.password", serviceUser.password);
    }

    public static void configureLdap(LdapConfig ldapConfig) {
        setProperty("ldap.username", ldapConfig.username);
        setProperty("ldap.password", ldapConfig.username);
        setProperty("ldap.url","ldaps\\://ldapgw.test.local");
        setProperty("ldap.basedn","dc\\=test,dc\\=local");
        setProperty("abac.endpoint.url", String.format("https://wasapp-%s.adeo.no/asm-pdp/authorize", ldapConfig.environment));
    }

    public static void setupIntegrationTestSecurity(ServiceUser serviceUser) {
        commonSetup();

        configureServiceUser(serviceUser);
        configureAbacUser(serviceUser);
        configureSubjectHandler(StaticSubjectHandler.class);
        StaticSubjectHandler subjectHandler = (StaticSubjectHandler) StaticSubjectHandler.getSubjectHandler();
        subjectHandler.setSubject(integrationTestSubject(serviceUser));
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
        TestCertificates.setupKeyAndTrustStore();
        setProperty("environment.class", "t");
        setProperty("disable.ssl.cn.check", Boolean.TRUE.toString());
        setProperty("disable.metrics.report", Boolean.TRUE.toString());
        setProperty("jetty.print.localhost", Boolean.TRUE.toString());
        setProperty(AnnotationConfiguration.MAX_SCAN_WAIT, "120");
    }

    private static void assertCorrectDomain(ServiceUser serviceUser, String expectedDomain) {
        if(!expectedDomain.equals(serviceUser.domain)){
            throw new IllegalStateException();
        }
    }

    private static void configureServiceUser(ServiceUser serviceUser) {
        setProperty("no.nav.modig.security.systemuser.username", serviceUser.username);
        setProperty("no.nav.modig.security.systemuser.password", serviceUser.password);
        setProperty("no.nav.modig.security.sts.url", format("https://sts-%s.%s/SecurityTokenServiceProvider/", serviceUser.environment, serviceUser.domain));
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

    @SneakyThrows
    public static <T> T configureSubjectHandler(Class<T> subjectHandlerClass) {
        String subjectHandlerClassName = subjectHandlerClass.getName();
        LOG.info("{} -> {}", SubjectHandler.class.getName(), subjectHandlerClassName);
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", subjectHandlerClassName);
        return subjectHandlerClass.newInstance();
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
