package no.nav.brukerdialog.security.oidc;

import org.junit.Test;

import java.util.Base64;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class TokenUtilsTest {

    private String testTokenMultipleAudBody = "{\"aud\":\"[testaud1,testaud2]\", \"azp\":\"testazp\"}";
    private String testTokenSingleAudBody = "{\"aud\":\"testaud\", \"azp\":\"testazp\"}";
    private String testTokenNoAzpBody = "{\"aud\":\"testaud\"}";

    @Test
    public void skalBrukeAudSomOpenamClient() {
        String encodedToken = getEncodedToken(testTokenNoAzpBody);

        String openamClient = TokenUtils.getOpenamClientFromToken(encodedToken);

        assertThat(openamClient).isEqualTo("testaud");
    }

    @Test
    public void skalBrukeAzpSomOpenamClient() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String openamClient = TokenUtils.getOpenamClientFromToken(encodedToken);

        assertThat(openamClient).isEqualTo("testazp");
    }

    @Test
    public void skalHenteAzpFraToken() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String azp = TokenUtils.getTokenAzp(encodedToken);

        assertThat(azp).isEqualTo("testazp");
    }

    @Test
    public void skalHenteAudFraToken() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String aud = TokenUtils.getTokenAud(encodedToken);

        assertThat(aud).isEqualTo("testaud");
    }

    @Test
    public void skalHenteForsteAudIListe() {
        String encodedToken = getEncodedToken(testTokenMultipleAudBody);

        String aud = TokenUtils.getTokenAud(encodedToken);

        assertThat(aud).isEqualTo("testaud1");


    }

    private String getEncodedToken(String tokenBody) {
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedTokenBody = new String(encoder.encode(tokenBody.getBytes()));
        return "dummy."+encodedTokenBody+".dummy";
    }
}