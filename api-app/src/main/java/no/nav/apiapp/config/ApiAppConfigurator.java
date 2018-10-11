package no.nav.apiapp.config;

import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;

import java.util.function.Consumer;

public interface ApiAppConfigurator {
    ApiAppConfigurator sts();
    ApiAppConfigurator sts(StsConfig stsConfig);

    ApiAppConfigurator issoLogin();
    ApiAppConfigurator issoLogin(IssoConfig issoConfig);
    ApiAppConfigurator azureADB2CLogin();
    ApiAppConfigurator azureADB2CLogin(AzureADB2CConfig azureADB2CConfig);
    ApiAppConfigurator openAmLogin();
    ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig);
    ApiAppConfigurator addPublicPath(String path);

    ApiAppConfigurator customizeJetty(Consumer<Jetty> jettyCustomizer);
    ApiAppConfigurator customizeJettyBuilder(Consumer<JettyBuilder> jettyBuilderCustomizer);
    ApiAppConfigurator addBean(Object bean);
}
