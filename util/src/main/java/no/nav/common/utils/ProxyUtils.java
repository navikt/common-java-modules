package no.nav.common.utils;

public class ProxyUtils {

    public static void useSystemProxy() {
        System.setProperty("java.net.useSystemProxies", "true");
    }

}
