package no.nav.common.utils;

import org.junit.Test;

import static no.nav.common.utils.EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME;
import static no.nav.common.utils.UrlUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class UrlUtilsTest {

    @Test
    public void test_clusterUrlForApplication() {
        System.clearProperty(NAIS_NAMESPACE_PROPERTY_NAME);
        assertThatThrownBy(() -> clusterUrlForApplication("app1")).hasMessageContaining(NAIS_NAMESPACE_PROPERTY_NAME);

        System.setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "q0");
        assertThat(clusterUrlForApplication("app1")).isEqualTo("http://app1.q0.svc.nais.local");
        assertThat(clusterUrlForApplication("app2")).isEqualTo("http://app2.q0.svc.nais.local");

        System.setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "default");
        assertThat(clusterUrlForApplication("app1")).isEqualTo("http://app1.default.svc.nais.local");
        assertThat(clusterUrlForApplication("app2")).isEqualTo("http://app2.default.svc.nais.local");
    }

    @Test
    public void createAppAdeoPreprodIngressUrl__should_create_ingress_with_context_path() {
        assertEquals(createAppAdeoPreprodIngressUrl("veilarbtest", "q1"), "https://app-q1.adeo.no/veilarbtest");
    }

    @Test
    public void createAppAdeoProdIngressUrl__should_create_ingress_with_context_path() {
        assertEquals(createAppAdeoProdIngressUrl("veilarbtest"), "https://app.adeo.no/veilarbtest");
    }

    @Test
    public void createNaisAdeoIngressUrl__should_create_ingress_with_context_path() {
        assertEquals(createNaisAdeoIngressUrl("veilarbtest", true), "https://veilarbtest.nais.adeo.no/veilarbtest");
    }

    @Test
    public void createNaisAdeoIngressUrl__should_create_ingress_without_context_path() {
        assertEquals(createNaisAdeoIngressUrl("veilarbtest", false), "https://veilarbtest.nais.adeo.no");
    }

    @Test
    public void createDevAdeoIngressUrl__should_create_ingress_with_context_path() {
        assertEquals(createDevAdeoIngressUrl("veilarbtest", true), "https://veilarbtest.dev.adeo.no/veilarbtest");
    }

    @Test
    public void createDevAdeoIngressUrl__should_create_ingress_without_context_path() {
        assertEquals(createDevAdeoIngressUrl("veilarbtest", false), "https://veilarbtest.dev.adeo.no");
    }

    @Test
    public void createNaisPreprodIngressUrl__should_create_ingress_with_context_path() {
        assertEquals(createNaisPreprodIngressUrl("veilarbtest",  "q1", true), "https://veilarbtest-q1.nais.preprod.local/veilarbtest");
    }

    @Test
    public void createNaisPreprodIngressUrl__should_create_ingress_without_context_path() {
        assertEquals(createNaisPreprodIngressUrl("veilarbtest", "q1", false), "https://veilarbtest-q1.nais.preprod.local");
    }

    @Test
    public void test_sluttMedSlash() {
        assertThat(sluttMedSlash(null)).isEqualTo("/");
        assertThat(sluttMedSlash("")).isEqualTo("/");
        assertThat(sluttMedSlash("/")).isEqualTo("/");
        assertThat(sluttMedSlash("/abc")).isEqualTo("/abc/");
    }

    @Test
    public void test_startMedSlash() {
        assertThat(startMedSlash(null)).isEqualTo("/");
        assertThat(startMedSlash("")).isEqualTo("/");
        assertThat(startMedSlash("abc")).isEqualTo("/abc");
        assertThat(startMedSlash("/abc")).isEqualTo("/abc");
    }


    @Test
    public void test_joinPaths() {
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
