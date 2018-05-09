package no.nav.sbl.dialogarena.test;

import lombok.SneakyThrows;

import java.net.InetAddress;

public class WebProxyConfigurator {

    @SneakyThrows
    public static void setupWebProxy() {
        String localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        System.setProperty("http.nonProxyHosts", localHostName + "|localhost|127.0.0.1|0.0.0.0|*.155.55.|*.192.168.|*.10.|*.local|*.rtv.gov|*.adeo.no|*.nav.no|*.aetat.no|*.devillo.no|*.oera.no");
        System.setProperty("http.proxyHost", "webproxy-utvikler.nav.no");
        System.setProperty("http.proxyPort", "8088");
        System.setProperty("https.proxyHost", "webproxy-utvikler.nav.no");
        System.setProperty("https.proxyPort", "8088");
    }

}
