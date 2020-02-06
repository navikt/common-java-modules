package no.nav.apiapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SecurityLevel;
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


    ApiAppConfigurator azureADB2CLogin(AzureADB2CConfig azureADB2CConfig);

    ApiAppConfigurator azureADB2CLogin();


    ApiAppConfigurator openAmLogin();
    ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig);

    ApiAppConfigurator validateAzureAdExternalUserTokens();

    /**
     * @param defaultSecurityLevel default security level in authorization of security level for external users applied to all secured paths.
     * @see ApiAppConfigurator#customSecurityLevelForExternalUsers(SecurityLevel, String...) for granular configuration.
     */
    ApiAppConfigurator validateAzureAdExternalUserTokens(SecurityLevel defaultSecurityLevel);

    /**
     * Configure granular authorization of security level for external users applied to paths starting with basePath.
     * <p>
     * Example:
     * <pre><code class='java'>
     * // Level3 for path /level3, including  all sub-paths, such as /level3/a and /level3/a/1:
     * customSecurityLevelForExternalUsers(SecurityLevel.Level3, "level3")
     * </code></pre>
     * @param securityLevel target security level
     * @param basePath requiring target security level for to paths starting with basePath
     */
    ApiAppConfigurator customSecurityLevelForExternalUsers(SecurityLevel securityLevel, String... basePath);

    ApiAppConfigurator validateAzureAdInternalUsersTokens();

    ApiAppConfigurator validateAzureAdInternalUsersTokens(AzureADB2CConfig config);

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
