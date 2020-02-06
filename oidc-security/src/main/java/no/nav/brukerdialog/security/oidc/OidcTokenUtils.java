package no.nav.brukerdialog.security.oidc;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.SsoToken;
import no.nav.json.JsonUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static no.nav.common.auth.SecurityLevel.*;
import static org.jose4j.jwt.ReservedClaimNames.AUDIENCE;
import static org.jose4j.jwt.ReservedClaimNames.SUBJECT;

@Slf4j
public class OidcTokenUtils {

    public static final String SECURITY_LEVEL_ATTRIBUTE = "acr";

    public static String getOpenamClientFromToken(String token) {
        return ofNullable(getTokenAzp(token))
                .orElse(getTokenAud(token));
    }

    public static String getTokenAud(String token) {
        return getFieldFromToken(token, AUDIENCE, value -> {
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof List) {
                List list = (List) value;
                return list.isEmpty() ? null : (String) list.get(0);
            } else {
                return null;
            }
        });
    }

    public static String getTokenAzp(String token) {
        return getStringFieldFromToken(token, "azp");
    }

    public static String getTokenSub(String token) {
        return getStringFieldFromToken(token, SUBJECT);
    }

    public static SecurityLevel getOidcSecurityLevel(String token) {
        return levelFromAcr(getStringFieldFromToken(token, SECURITY_LEVEL_ATTRIBUTE));
    }

    public static SecurityLevel getOidcSecurityLevel(SsoToken ssoToken) {
        return ssoToken.getType() != SsoToken.Type.OIDC ? Ukjent : ofNullable(ssoToken.getAttributes())
                .map(a -> a.get(SECURITY_LEVEL_ATTRIBUTE))
                .map(o -> o instanceof String ? (String) o : null)
                .map(OidcTokenUtils::levelFromAcr)
                .orElse(Ukjent);
    }

    private static SecurityLevel levelFromAcr(String acr) {
        if (acr == null) {
            return Ukjent;
        }

        switch (acr) {
            case "Level1":
                return Level1;
            case "Level2":
                return Level2;
            case "Level3":
                return Level3;
            case "Level4":
                return Level4;
            default:
                return Ukjent;
        }
    }

    public static String getStringFieldFromToken(String token, String field) {
        return getFieldFromToken(token, field, String.class::cast);
    }

    private static <T> T getFieldFromToken(String token, String field, Function<Object, T> converter) {
        try {
            String tokenBody = token.split("\\.")[1];
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String bodyDecoded = new String(decoder.decode(tokenBody));
            return converter.apply(JsonUtils.fromJson(bodyDecoded, Map.class).get(field));
        } catch (Exception e) {
            log.warn("Kunne ikke hente {} fra token", field);
            return null;
        }
    }

}
