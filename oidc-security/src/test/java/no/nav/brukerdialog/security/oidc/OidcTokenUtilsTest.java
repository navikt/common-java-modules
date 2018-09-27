package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.SecurityLevel;
import org.junit.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;


public class OidcTokenUtilsTest {

    private String testTokenMultipleAudBody = "{\"aud\":\"[testaud1,testaud2]\", \"azp\":\"testazp\"}";
    private String testTokenSingleAudBody = "{\"aud\":\"testaud\", \"azp\":\"testazp\"}";
    private String testTokenNoAzpBody = "{\"aud\":\"testaud\"}";
    private String testTokenWithSecurityLevel = "{\"acr\":\"Level4\"}";

    @Test
    public void skalBrukeAudSomOpenamClient() {
        String encodedToken = getEncodedToken(testTokenNoAzpBody);

        String openamClient = OidcTokenUtils.getOpenamClientFromToken(encodedToken);

        assertThat(openamClient).isEqualTo("testaud");
    }

    @Test
    public void skalBrukeAzpSomOpenamClient() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String openamClient = OidcTokenUtils.getOpenamClientFromToken(encodedToken);

        assertThat(openamClient).isEqualTo("testazp");
    }

    @Test
    public void skalHenteAzpFraToken() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String azp = OidcTokenUtils.getTokenAzp(encodedToken);

        assertThat(azp).isEqualTo("testazp");
    }

    @Test
    public void skalHenteAudFraToken() {
        String encodedToken = getEncodedToken(testTokenSingleAudBody);

        String aud = OidcTokenUtils.getTokenAud(encodedToken);

        assertThat(aud).isEqualTo("testaud");
    }

    @Test
    public void skalSikkerhetsnivaFraToken() {
        String encodedToken = getEncodedToken(testTokenWithSecurityLevel);

        SecurityLevel secLevel = OidcTokenUtils.getOidcSecurityLevel(encodedToken);

        assertThat(secLevel.getSecurityLevel()).isEqualTo(4);
        assertThat(secLevel.getSecurityLevel()).isNotEqualTo(2);
    }

    @Test
    public void skalHenteForsteAudIListe() {
        String encodedToken = getEncodedToken(testTokenMultipleAudBody);

        String aud = OidcTokenUtils.getTokenAud(encodedToken);

        assertThat(aud).isEqualTo("testaud1");
    }

    private String getEncodedToken(String tokenBody) {
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedTokenBody = new String(encoder.encode(tokenBody.getBytes()));
        return "dummy."+encodedTokenBody+".dummy";
    }
}