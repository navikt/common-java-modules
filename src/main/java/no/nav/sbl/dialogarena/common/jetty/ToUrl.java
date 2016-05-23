package no.nav.sbl.dialogarena.common.jetty;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.function.Function;

public class ToUrl implements Function<Integer, URL> {

    private final String scheme;
    private final String path;

    public ToUrl(String scheme, String path) {
        this.scheme = scheme;
        this.path = (path.startsWith("/") ? "" : "/") + path;
    }

    @Override
    public URL apply(Integer port) {
        try {
            return new URL(scheme + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + port + path);
        } catch (MalformedURLException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}