package no.nav.fo.apiapp;

import no.nav.apiapp.config.Konfigurator;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.testconfig.ApiAppTest;

public class ApiAppTestMain {

    public static void main(String[] args) throws Exception {
        ApiAppTest.setupTestContext();

        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService");
        ServiceUser srvveilarbdemo = FasitUtils.getServiceUser("srvveilarbdemo", "veilarbdemo");

        System.setProperty(StsSecurityConstants.STS_URL_KEY, securityTokenService);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());

        System.setProperty(Konfigurator.OPENAM_RESTURL, "https://itjenester-" + FasitUtils.getDefaultTestEnvironment().toString() + ".oera.no/esso");

        ApiAppMain.main();
    }

}
