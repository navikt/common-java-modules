package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.config.IssoConfig;
import no.nav.apiapp.config.OpenAmConfig;
import no.nav.modig.core.context.ModigSecurityConstants;
import no.nav.modig.security.loginmodule.OpenAMLoginModule;
import no.nav.modig.security.loginmodule.SamlLoginModule;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.util.EnvironmentUtils;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.slf4j.Logger;

import javax.security.auth.spi.LoginModule;

import static no.nav.apiapp.config.Konfigurator.OPENAM_RESTURL;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class LoginConfigurator {

    private static final Logger LOG = getLogger(LoginConfigurator.class);

    @SneakyThrows
    public static void setupSamlLogin(Jetty.JettyBuilder jettyBuilder) {
        modigSubjectHandler();
        dialogArenaSubjectHandler();
        LOG.info("configuring: {}", SamlLoginModule.class.getName());
        setLoginService(jettyBuilder, LoginModuleType.SAML);
    }

    @SneakyThrows
    public static void setupOpenAmLogin(Jetty.JettyBuilder jettyBuilder, OpenAmConfig openAmConfig) {
        setProperty(OPENAM_RESTURL, openAmConfig.restUrl, PUBLIC);
        setProperty(ModigSecurityConstants.SYSTEMUSER_USERNAME, openAmConfig.username, PUBLIC);
        setProperty(ModigSecurityConstants.SYSTEMUSER_PASSWORD, openAmConfig.password, SECRET);

        modigSubjectHandler();
        setLoginService(jettyBuilder, LoginModuleType.ESSO);
    }

    @SneakyThrows
    public static void setupIssoLogin(Jetty.JettyBuilder jettyBuilder, IssoConfig issoConfig) {
        jettyBuilder.configureForJaspic(issoConfig.ubeskyttet);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, issoConfig.username, PUBLIC);
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, issoConfig.password, SECRET);
        dialogArenaSubjectHandler();
    }

    private static void setLoginService(Jetty.JettyBuilder jettyBuilder, LoginModuleType loginModuleType) {
        LOG.info("configuring: {}", loginModuleType.loginModuleClass);
        jettyBuilder.withLoginService(jaasLoginModule(loginModuleType));
    }

    private static JAASLoginService jaasLoginModule(LoginModuleType loginModuleType) {
        String jaasConfig = LoginConfigurator.class.getResource("/api-app/jaas.config").toExternalForm();
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
