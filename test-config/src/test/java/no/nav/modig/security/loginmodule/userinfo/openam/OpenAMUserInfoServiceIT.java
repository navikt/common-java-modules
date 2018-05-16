package no.nav.modig.security.loginmodule.userinfo.openam;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.security.ESSOProvider;
import no.nav.modig.security.loginmodule.userinfo.UserInfo;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

public class OpenAMUserInfoServiceIT {

    @Test
    public void testGetUserInfo() throws IOException {
        String token = ESSOProvider.getHttpCookie().getValue();
        System.out.println("TOKEN:" + token);

        String endpoint = "https://itjenester-" + FasitUtils.getDefaultEnvironment() + ".oera.no/esso";
        UserInfo info = new OpenAMUserInfoService(URI.create(endpoint)).getUserInfo(token);
        System.out.println("Uid:" + info.getUid());
        System.out.println("Authlevel:" + info.getAuthLevel());
    }

}
