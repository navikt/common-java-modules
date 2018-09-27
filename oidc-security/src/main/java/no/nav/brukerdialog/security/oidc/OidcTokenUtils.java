package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.SecurityLevel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static java.util.Optional.ofNullable;
import static no.nav.brukerdialog.security.SecurityLevel.*;


public class OidcTokenUtils {

    private static final Logger log = LoggerFactory.getLogger(OidcTokenUtils.class);

    public static String getOpenamClientFromToken(String token) {
        return ofNullable(getTokenAzp(token))
                .orElse(getTokenAud(token));
    }

    public static String getTokenAud(String token) {
        try {
            String tokenBody = token.split("\\.")[1];
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String bodyDecoded = new String(decoder.decode(tokenBody));
            String audString = new JSONObject(bodyDecoded).getString("aud");
            return audString.replaceAll("\\[|]","").split(",")[0];
        } catch(Exception e ) {
            log.warn("Kunne ikke hente aud fra token");
            return null;
        }
    }

    public static String getTokenAzp(String token) {
        return getFieldFromToken(token, "azp");
    }

    public static String getTokenSub(String token) {
        return getFieldFromToken(token, "sub");
    }

    public static SecurityLevel getOidcSecurityLevel(String token) {
        String acr = getFieldFromToken(token, "acr");

        if (acr == null) {
            return Ukjent;
        }

        switch (acr) {
            case "Level1": return Level1;
            case "Level2": return Level2;
            case "Level3": return Level3;
            case "Level4": return Level4;
            default: return Ukjent;
        }
    }

    private static String getFieldFromToken(String token, String field) {
        try {
            String tokenBody = token.split("\\.")[1];
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String bodyDecoded = new String(decoder.decode(tokenBody));
            return new JSONObject(bodyDecoded).getString(field);
        } catch(Exception e ) {
            log.warn("Kunne ikke hente {} fra token", field);
            return null;
        }
    }

}
