package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.apiapp.auth.OidcAuthenticatorConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import no.nav.sbl.dialogarena.types.Pingable;

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
