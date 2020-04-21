package no.nav.common.testconfig.security;

import org.jose4j.jwk.JsonWebKeySet;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JwtTestTokenUtil {

    public static JwtTestTokenIssuer testTokenIssuer(JwtTestTokenIssuerConfig config) {
        return new JwtTestTokenIssuer(config);
    }

    public static String getKeySetJson(JwtTestTokenIssuer... issuers) {
        return new JsonWebKeySet(Arrays.stream(issuers).map(x -> x.key).collect(Collectors.toList())).toJson();
    }

}
