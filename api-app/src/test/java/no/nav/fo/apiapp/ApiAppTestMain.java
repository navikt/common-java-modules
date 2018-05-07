package no.nav.fo.apiapp;

import no.nav.apiapp.config.Konfigurator;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.testconfig.ApiAppTest;

import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME;
import static no.nav.dialogarena.config.util.Util.setProperty;

public class ApiAppTestMain {

    public static void main(String[] args) throws Exception {
        ApiAppTest.setupTestContext();

        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService");
        ServiceUser srvveilarbdemo = FasitUtils.getServiceUser("srvveilarbdemo", "veilarbdemo");

        setProperty(StsSecurityConstants.STS_URL_KEY, securityTokenService);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());

        setProperty(Konfigurator.OPENAM_RESTURL, "https://itjenester-" + FasitUtils.getDefaultTestEnvironment().toString() + ".oera.no/esso");

        ServiceUser azureADClientId = FasitUtils.getServiceUser("aad_b2c_clientid", "veilarbdemo");
        setProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME, FasitUtils.getBaseUrl("aad_b2c_discovery"));
        setProperty(AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME, azureADClientId.username);

        ApiAppMain.main();
    }

}
