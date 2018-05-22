package no.nav.fo.apiapp;

import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
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
        RestService abacEndpoint = FasitUtils.getRestService("abac.pdp.endpoint", srvveilarbdemo.getEnvironment());

        setProperty(StsSecurityConstants.STS_URL_KEY, securityTokenService);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());

        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());
        setProperty(AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME, abacEndpoint.getUrl());

        setProperty(OpenAmConfig.OPENAM_RESTURL, "https://itjenester-" + FasitUtils.getDefaultTestEnvironment().toString() + ".oera.no/esso");

        ServiceUser azureADClientId = FasitUtils.getServiceUser("aad_b2c_clientid", "veilarbdemo");
        setProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME, FasitUtils.getBaseUrl("aad_b2c_discovery"));
        setProperty(AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME, azureADClientId.username);

        ApiAppMain.main();
    }

}
