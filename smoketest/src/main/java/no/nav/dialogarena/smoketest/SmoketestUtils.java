package no.nav.dialogarena.smoketest;

import java.util.Objects;

public class SmoketestUtils {

    public static String appOrLocalhost(String miljo) {
        return Objects.nonNull(miljo) ? String.format("https://app-%s.adeo.no/", miljo) : "http://localhost:8080/";
    }
}
