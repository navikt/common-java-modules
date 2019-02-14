package no.nav.brukerdialog.security;


import no.nav.brukerdialog.security.oidc.OidcTokenException;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CProvider;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.sbl.dialogarena.test.WebProxyConfigurator;

import javax.script.ScriptException;
import java.io.IOException;

import static java.lang.System.setProperty;
import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;

public class OidcTestRunner {

    public static void main(String[] args) throws InterruptedException, ScriptException, IOException, OidcTokenException {
        WebProxyConfigurator.setupWebProxy();

        // SETUP
        String applicationName = "veilarbdemo";
        setProperty(APP_NAME_PROPERTY_NAME,applicationName);

        String issoHost = FasitUtils.getBaseUrl("isso-host");
        String issoJWS = FasitUtils.getBaseUrl("isso-jwks");
        String issoISSUER = FasitUtils.getBaseUrl("isso-issuer");
        String issoIsAlive = FasitUtils.getBaseUrl("isso.isalive", FasitUtils.Zone.FSS);
        ServiceUser srvveilarbdirigent = FasitUtils.getServiceUser("srvveilarbdemo", applicationName);
        ServiceUser isso_rp_user = FasitUtils.getServiceUser("isso-rp-user", applicationName);
        String redirectlUrl = FasitUtils.getRestService("veilarblogin.redirect-url", FasitUtils.getDefaultEnvironment()).getUrl();

        setProperty(Constants.ISSO_HOST_URL_PROPERTY_NAME, issoHost);
        setProperty(Constants.ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(Constants.ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(Constants.ISSO_JWKS_URL_PROPERTY_NAME, issoJWS);
        setProperty(Constants.ISSO_ISSUER_URL_PROPERTY_NAME, issoISSUER);
        setProperty(Constants.ISSO_ISALIVE_URL_PROPERTY_NAME, issoIsAlive);
        setProperty(Constants.OIDC_REDIRECT_URL_PROPERTY_NAME, redirectlUrl);
        setProperty(SecurityConstants.SYSTEMUSER_USERNAME, srvveilarbdirigent.getUsername());
        setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, srvveilarbdirigent.getPassword());

        OidcTokenValidator oidcTokenValidator = new OidcTokenValidator();

        // ISSO
        SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();
        String idToken = systemUserTokenProvider.getToken();
        System.out.println("idToken: " + idToken);

        IssoOidcProvider issoOidcProvider = new IssoOidcProvider();
        printResult(oidcTokenValidator.validate(idToken, issoOidcProvider));

        // ESSO
        ServiceUser serviceUser = FasitUtils.getServiceUser("aad_b2c_clientid", applicationName);
        String aad_b2c_discovery = FasitUtils.getBaseUrl("aad_b2c_discovery");
        AzureADB2CProvider azureADB2CProvider = new AzureADB2CProvider(AzureADB2CConfig.builder()
                .discoveryUrl(aad_b2c_discovery)
                .expectedAudience(serviceUser.username)
                .build()
        );

        // utløpt ikke-sensitivt test-token
        String testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImZ5akpfczQwN1ZqdnRzT0NZcEItRy1IUTZpYzJUeDNmXy1JT3ZqVEFqLXcifQ.eyJleHAiOjE1MjQwNTA1MjQsIm5iZiI6MTUyNDA0NjkyNCwidmVyIjoiMS4wIiwiaXNzIjoiaHR0cHM6Ly9sb2dpbi5taWNyb3NvZnRvbmxpbmUuY29tL2QzOGYyNWFhLWVhYjgtNGM1MC05ZjI4LWViZjkyYzEyNTZmMi92Mi4wLyIsInN1YiI6IjI4MDE3OTE4OTI1IiwiYXVkIjoiMDA5MGI2ZTEtZmZjYy00YzM3LWJjMjEtMDQ5ZjdkMWYwZmU1IiwiYWNyIjoiTGV2ZWw0Iiwibm9uY2UiOiJhZUF1WFc5WUNaZVJnX1NMN0VWTUJkYUtROUhlQXdCTkQ5UW8wQkYxUlQ4IiwiaWF0IjoxNTI0MDQ2OTI0LCJhdXRoX3RpbWUiOjE1MjQwNDY5MjQsImp0aSI6IjdqQnRvRzhSRThIZDFsTUw2dTc4dC1qZ0IzeXRBYnJhcmpFTG05QmdXMkU9IiwiYXRfaGFzaCI6IlRnSkN2VEQxQ1dSRUdaZEV5VjR6REEifQ.T8wfwzRTAD82Nx1yvZXOZ-FwyWBQsVcSJfzka-a1BcVtaF2bS1oBZQpc-r_eXCq4UG4uJMaGPTRoUxXh_dCwYEzPLci_I7dvTN4rSLgn-0Lzii1F6Rb6Fwt-3PWgvc8YXzW9Zzaf_UYNFdMm5nW-KqnF5nksOgTYkWv1czstNRrykecgFNK_ZGCmNBhXF5kPEjGzD_sQcsfiitswvcYzfIlTSjdCPDOEAaLC-xYyRwdm9mIc1gJI7ueq2COnsNKvc2rM1ZtfbzLS6iojGbwStk1lc6xc00feg5qQ1VwIzDJ4E1lAtp1Ek1g_5FAVQgyW-BB_TE4vLoWaro7oQz74Qg";
        printResult(oidcTokenValidator.validate(testToken, azureADB2CProvider));
    }

    private static void printResult(OidcTokenValidatorResult tokenValidatorResult) {
        boolean valid = tokenValidatorResult.isValid();
        System.out.println(valid);
        if (valid) {
            System.out.println(tokenValidatorResult.getSubject());
            System.out.println(tokenValidatorResult.getExpSeconds());
        } else {
            System.out.println(tokenValidatorResult.getErrorMessage());
        }
    }

}
