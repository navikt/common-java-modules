package no.nav.sbl.dialogarena.test;

public class WebProxyConfigurator {

    public static void setupWebProxy() {
        System.setProperty("http.nonProxyHosts", "*.155.55.|*.192.168.|*.10.|*.local|*.rtv.gov|*.adeo.no|*.nav.no|*.aetat.no|*.devillo.no|*.oera.no");
        System.setProperty("http.proxyHost", "webproxy-utvikler.nav.no");
        System.setProperty("http.proxyPort", "8088");
        System.setProperty("https.proxyHost", "webproxy-utvikler.nav.no");
        System.setProperty("https.proxyPort", "8088");
    }

}
