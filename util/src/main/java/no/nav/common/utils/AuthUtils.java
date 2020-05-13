package no.nav.common.utils;

import java.util.Base64;

public class AuthUtils {

    public static String basicCredentials(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes());
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }

}
