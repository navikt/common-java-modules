package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.apiapp.auth.OidcAuthenticatorConfig;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import no.nav.sbl.dialogarena.types.Pingable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.function.Consumer;

public interface ApiAppConfigurator {
    ApiAppConfigurator sts();
    ApiAppConfigurator sts(StsConfig stsConfig);

    ApiAppConfigurator addOidcAuthenticator(OidcAuthenticatorConfig config);

    ApiAppConfigurator addPublicPath(String path);

    ApiAppConfigurator customizeJetty(Consumer<Jetty> jettyCustomizer);
    ApiAppConfigurator customizeJettyBuilder(Consumer<JettyBuilder> jettyBuilderCustomizer);

    ApiAppConfigurator selfTest(Pingable pingable);
    ApiAppConfigurator selfTests(Pingable... pingables);
    ApiAppConfigurator selfTests(Collection<? extends Pingable> pingables);

    ApiAppConfigurator objectMapper(ObjectMapper objectMapper);

}
