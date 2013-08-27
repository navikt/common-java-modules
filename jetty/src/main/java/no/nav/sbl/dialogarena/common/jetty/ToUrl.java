package no.nav.sbl.dialogarena.common.jetty;

import org.apache.commons.collections15.Transformer;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class ToUrl implements Transformer<Integer, URL> {

    private final String scheme;
    private final String path;

    public ToUrl(String scheme, String path) {
        this.scheme = scheme;
        this.path = path + (path.startsWith("/") ? "" : "/");
    }

    @Override
    public URL transform(Integer port) {
        try {
            return new URL(scheme + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + port + path);
        } catch (MalformedURLException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}