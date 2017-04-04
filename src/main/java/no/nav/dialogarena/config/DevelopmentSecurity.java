package no.nav.dialogarena.config;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.context.JettySubjectHandler;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.apache.commons.io.IOUtils;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;
import org.slf4j.Logger;

import javax.security.auth.message.config.AuthConfigFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.String.format;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static org.apache.commons.io.IOUtils.write;
import static org.slf4j.LoggerFactory.getLogger;

public class DevelopmentSecurity {

    private static final Logger LOG = getLogger(DevelopmentSecurity.class);

    @Setter
    @Accessors(chain = true)
    public static class ISSOSecurityConfig {

        private final String applicationName;
        private final String environment;

        private String issoUserName;
        private String serviceUserName;
        private String contextName;

        public ISSOSecurityConfig(String applicationName, String environment) {
            this.applicationName = applicationName;
            this.environment = environment;

            this.contextName = applicationName;
            this.issoUserName = "isso-rp-user";
            this.serviceUserName = "srv" + applicationName;
        }
    }

    @SneakyThrows
    public static Jetty.JettyBuilder setupISSO(Jetty.JettyBuilder jettyBuilder, ISSOSecurityConfig issoSecurityConfig) {
        setProperty("disable.metrics.report", Boolean.TRUE.toString());

        String environment = issoSecurityConfig.environment;
        String environmentShort = environment.substring(0,1);

        ServiceUser issoCredentials = FasitUtils.getServiceUser(issoSecurityConfig.issoUserName, issoSecurityConfig.applicationName, environment);

        setProperty("isso-rp-user.username", issoCredentials.username);
        setProperty("isso-rp-user.password", issoCredentials.password);
        setProperty("isso-jwks.url", format("https://isso-%s.adeo.no/isso/oauth2/connect/jwk_uri", environmentShort));
        setProperty("isso-issuer.url", format("https://isso-%s.adeo.no:443/isso/oauth2", environmentShort)); // OBS OBS, må sette port 443 her av en eller annen merkelig grunn!
        setProperty("isso-host.url", format("https://isso-%s.adeo.no/isso/oauth2", environmentShort));

        ServiceUser serviceUser = FasitUtils.getServiceUser(issoSecurityConfig.serviceUserName, issoSecurityConfig.applicationName, environment);
        setProperty("no.nav.abac.systemuser.username", serviceUser.username);
        setProperty("no.nav.abac.systemuser.password", serviceUser.password);

        setProperty("no.nav.modig.security.systemuser.username", serviceUser.username);
        setProperty("no.nav.modig.security.systemuser.password", serviceUser.password);
        setProperty("no.nav.modig.security.sts.url", format("https://sts-%s.test.local/SecurityTokenServiceProvider/", environment));


        String jettySubjectHandler = JettySubjectHandler.class.getName();
        LOG.info("{} -> {}", SubjectHandler.class.getName(), jettySubjectHandler);
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", jettySubjectHandler);

        setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());

        return configureJaspi(jettyBuilder, issoSecurityConfig);
    }

    private static Jetty.JettyBuilder configureJaspi(Jetty.JettyBuilder jettyBuilder, ISSOSecurityConfig issoSecurityConfig) throws IOException {
        File jaspiConfigFile = new File("target/jaspiconf.xml");
        try (FileOutputStream fileOutputStream = new FileOutputStream(jaspiConfigFile)) {
            write(readJaspiConfig(issoSecurityConfig), fileOutputStream);
        }
        String absolutePath = jaspiConfigFile.getAbsolutePath();
        LOG.info("jaspi contiguration at: {}", absolutePath);
        setProperty("org.apache.geronimo.jaspic.configurationFile", absolutePath);
        return jettyBuilder.configureForJaspic();
    }

    private static String readJaspiConfig(ISSOSecurityConfig issoSecurityConfig) throws IOException {
        return IOUtils.toString(DevelopmentSecurity.class.getResourceAsStream("/jaspiconf.xml"))
                .replace("%%CONTEXT_PATH%%", issoSecurityConfig.contextName);
    }

}
