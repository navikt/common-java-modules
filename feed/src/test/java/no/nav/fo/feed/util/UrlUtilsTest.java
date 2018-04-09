package no.nav.fo.feed.util;

import org.junit.Test;

import static no.nav.fo.feed.util.UrlUtils.asUrl;
import static no.nav.fo.feed.util.UrlUtils.callbackUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class UrlUtilsTest {
    @Test
    public void skalStotteHttp() {
        String url = "http://app-t5.adeo.no/veilarbportefolje//api/feed/tilordninger";
        assertThat(asUrl(url), is(equalTo("http://app-t5.adeo.no/veilarbportefolje/api/feed/tilordninger")));
    }

    @Test
    public void skalStotteHttps() {
        String url = "https://app-t5.adeo.no/veilarbportefolje//api/feed/tilordninger";
        assertThat(asUrl(url), is(equalTo("https://app-t5.adeo.no/veilarbportefolje/api/feed/tilordninger")));
    }

    @Test
    public void skalStotteFlereFeil() {
        String url = "http://app-t5.adeo.no//veilarbportefolje//api/feed//tilordninger?id=hvaskjer&mer";
        assertThat(asUrl(url), is(equalTo("http://app-t5.adeo.no/veilarbportefolje/api/feed/tilordninger?id=hvaskjer&mer")));
    }

    @Test
    public void skalGiRiktigUrlForLokaltMiljo() {
        System.setProperty("environment.class", "lokalt");
        System.setProperty("feed.lokalt.callback.host", "https://mylocalhost:1234/skjera/");

        assertThat(callbackUrl("/approot", "/feedname"), is(equalTo(
                "https://mylocalhost:1234/skjera/approot/feed/feedname"
        )));
    }

    @Test
    public void skalGiRiktigUrlForT() {
        System.setProperty("environment.class", "t");
        System.setProperty("environment.name", "t4");

        assertThat(callbackUrl("/approot", "/feedname"), is(equalTo(
                "https://app-t4.adeo.no/approot/feed/feedname"
        )));
    }
}