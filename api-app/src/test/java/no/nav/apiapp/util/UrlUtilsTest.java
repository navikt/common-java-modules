package no.nav.apiapp.util;

import org.junit.Test;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static no.nav.apiapp.util.UrlUtils.startMedSlash;
import static org.assertj.core.api.Assertions.assertThat;


public class UrlUtilsTest {

    @Test
    public void sluttMedSlash_() {
        assertThat(sluttMedSlash(null)).isEqualTo("/");
        assertThat(sluttMedSlash("")).isEqualTo("/");
        assertThat(sluttMedSlash("/")).isEqualTo("/");
        assertThat(sluttMedSlash("/abc")).isEqualTo("/abc/");
    }

    @Test
    public void startMedSlash_() {
        assertThat(startMedSlash(null)).isEqualTo("/");
        assertThat(startMedSlash("")).isEqualTo("/");
        assertThat(startMedSlash("abc")).isEqualTo("/abc");
        assertThat(startMedSlash("/abc")).isEqualTo("/abc");
    }


    @Test
    public void joinPaths_() {
        assertThat(joinPaths(null)).isEqualTo("/");
        assertThat(joinPaths(null,null,null)).isEqualTo("/");
        assertThat(joinPaths("/","","/","/")).isEqualTo("/");
        assertThat(joinPaths("abc","def")).isEqualTo("/abc/def");
        assertThat(joinPaths("/abc","def")).isEqualTo("/abc/def");
        assertThat(joinPaths("abc/","def")).isEqualTo("/abc/def");
        assertThat(joinPaths("abc","/def")).isEqualTo("/abc/def");
        assertThat(joinPaths("abc","def/")).isEqualTo("/abc/def/");
        assertThat(joinPaths("/abc/","/def/")).isEqualTo("/abc/def/");
        assertThat(joinPaths("/abc/","","/def/")).isEqualTo("/abc/def/");
        assertThat(joinPaths("/abc/","/","/def/")).isEqualTo("/abc/def/");
        assertThat(joinPaths("http://","abc","def")).isEqualTo("http://abc/def");
        assertThat(joinPaths("http://abc","def","ghi")).isEqualTo("http://abc/def/ghi");
        assertThat(joinPaths("http://abc/","/def","ghi")).isEqualTo("http://abc/def/ghi");
        assertThat(joinPaths("http://abc/","def","ghi")).isEqualTo("http://abc/def/ghi");
        assertThat(joinPaths("http://abc/","def/","ghi")).isEqualTo("http://abc/def/ghi");
        assertThat(joinPaths("ftp://","/abc","def")).isEqualTo("ftp://abc/def");
    }

}