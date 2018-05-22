package no.nav.apiapp.config;

import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.common.auth.openam.sbs.OpenAmConfig;

public interface ApiAppConfigurator {
    ApiAppConfigurator sts();
    ApiAppConfigurator sts(StsConfig stsConfig);
    ApiAppConfigurator issoLogin();
    ApiAppConfigurator issoLogin(IssoConfig issoConfig);
    ApiAppConfigurator azureADB2CLogin();
    ApiAppConfigurator azureADB2CLogin(AzureADB2CConfig azureADB2CConfig);
    ApiAppConfigurator openAmLogin();
    ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig);
}
