package no.nav.sbl.dialogarena.common.jetty;

import org.junit.Test;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ToUrlTest {

    @Test
    public void transformToUrlWithLeadingSlash() throws UnknownHostException {
        ToUrl transformer = new ToUrl("http","/my/path");

        URL url = transformer.transform(8080);
        assertThat(url.toString(), equalTo("http://"+ InetAddress.getLocalHost().getCanonicalHostName()+":8080/my/path"));
    }

    @Test
    public void transformToUrlWithoutLeadingSlash() throws UnknownHostException {
        ToUrl transformer = new ToUrl("http","my/path");

        URL url = transformer.transform(8080);
        assertThat(url.toString(), equalTo("http://"+ InetAddress.getLocalHost().getCanonicalHostName()+":8080/my/path"));
    }

    @Test(expected = RuntimeException.class)
    public void transformMalformedUrl() {
        ToUrl transformer = new ToUrl("abc","");
        transformer.transform(1234);
    }
}
