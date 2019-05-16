package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import no.nav.sbl.dialogarena.types.Pingable;

import java.util.Collection;
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

    ApiAppConfigurator validateAzureAdExternalUserTokens();
    ApiAppConfigurator validateAzureAdInternalUsersTokens();

    ApiAppConfigurator securityTokenServiceLogin();
    ApiAppConfigurator securityTokenServiceLogin(SecurityTokenServiceOidcProviderConfig securityTokenServiceOidcProviderConfig);
    ApiAppConfigurator oidcProvider(OidcProvider oidcProvider);
    ApiAppConfigurator addPublicPath(String path);
    ApiAppConfigurator authorizationModule(AuthorizationModule authorizationModule);

    ApiAppConfigurator customizeJetty(Consumer<Jetty> jettyCustomizer);
    ApiAppConfigurator customizeJettyBuilder(Consumer<JettyBuilder> jettyBuilderCustomizer);

    ApiAppConfigurator selfTest(Pingable pingable);
    ApiAppConfigurator selfTests(Pingable... pingables);
    ApiAppConfigurator selfTests(Collection<? extends Pingable> pingables);

    ApiAppConfigurator objectMapper(ObjectMapper objectMapper);

}
