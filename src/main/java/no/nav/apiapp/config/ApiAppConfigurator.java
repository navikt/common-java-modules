package no.nav.apiapp.config;

public interface ApiAppConfigurator {
    ApiAppConfigurator sts();
    ApiAppConfigurator sts(StsConfig stsConfig);
    ApiAppConfigurator openAmLogin();
    ApiAppConfigurator openAmLogin(OpenAmConfig openAmConfig);
    ApiAppConfigurator samlLogin();
    ApiAppConfigurator samlLogin(SamlConfig samlConfig);
}
