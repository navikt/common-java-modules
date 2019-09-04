package no.nav.apiapp.rest;

import lombok.SneakyThrows;
import no.nav.common.utils.Pair;
import no.nav.sbl.util.fn.UnsafeRunnable;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static no.nav.apiapp.rest.NavCorsFilter.*;
import static no.nav.sbl.util.FunctionalUtils.sneakyFunction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


class NavCorsFilterTest {

    @Test
    public void validOrigin() {
        assertInvalidOrigin("origin", null);
        assertInvalidOrigin("", null);
        assertInvalidOrigin(null, null, null);
        assertInvalidOrigin("abcd.nav.no");
        assertInvalidOrigin("evil.com", ".nav.no");
        assertInvalidOrigin("evil.com", "", null);
        assertInvalidOrigin("abcd.nav.no", ".nav.noo");

        assertValidOrigin("abcd.nav.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".oera.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".oera.no", "", null, ".nav.no");
    }

    @Test
    public void hentKommaseparertListe() {
        System.clearProperty(CORS_ALLOWED_ORIGINS);
        assertThat(createCorsOrigin().value).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, "");
        assertThat(createCorsOrigin().value).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, ".nav.no,.oera.no");
        assertThat(createCorsOrigin().value).containsExactlyInAnyOrder(".nav.no", ".oera.no");
        System.setProperty(CORS_ALLOWED_ORIGINS, " .nav.no, .oera.no ");
        assertThat(createCorsOrigin().value).containsExactlyInAnyOrder(".nav.no", ".oera.no");
    }

    @Test
    public void krevSubdomene() {
        System.setProperty(CORS_ALLOWED_ORIGINS, "ikke.subdomene.no");
        assertThatThrownBy(NavCorsFilterTest::createCorsOrigin).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void leggerPaCorsHeadersHvisOriginMatcher() {
        HttpServletRequest request = getRequestMock("GET", "www.localhost.no");
        HttpServletResponse response = getResponseMock();
        FilterChain chain = getFilterChainMock();

        Map<String, String> corsSettings = corsSettings(
                Pair.of(CORS_ALLOWED_ORIGINS, ".localhost.no")
        );

        withCorsSettings(corsSettings, () -> {
            NavCorsFilter filter = new NavCorsFilter();

            filter.doFilter(request, response, chain);

            verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("www.localhost.no"));
            verify(response).setHeader(eq("Access-Control-Allow-Methods"), eq(String.join(", ", DEFAULT_ALLOWED_METHODS)));
            verify(response).setHeader(eq("Access-Control-Allow-Credentials"), eq("true"));
            verify(response).setHeader(eq("Access-Control-Max-Age"), eq("3600"));
            verify(response).setHeader(eq("Access-Control-Allow-Headers"), eq(String.join(", ", DEFAULT_ALLOWED_HEADERS)));
        });
    }

    @Test
    public void tarHensynTilDeUlikeMiljoVariablene() {
        HttpServletRequest request = getRequestMock("GET", "www.nav.no");
        HttpServletResponse response = getResponseMock();
        FilterChain chain = getFilterChainMock();

        Map<String, String> corsSettings = corsSettings(
                Pair.of(CORS_ALLOWED_ORIGINS, ".localhost.no, .nav.no"),
                Pair.of(CORS_ALLOWED_METHODS, "PUT"),
                Pair.of(CORS_ALLOWED_HEADERS, "Content-Type, Accept"),
                Pair.of(CORS_ALLOWED_CREDENTIALS, "false"),
                Pair.of(CORS_MAX_AGE, "60")
        );

        withCorsSettings(corsSettings, () -> {
            NavCorsFilter filter = new NavCorsFilter();

            filter.doFilter(request, response, chain);

            verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("www.nav.no"));
            verify(response).setHeader(eq("Access-Control-Allow-Methods"), eq("PUT"));
            verify(response).setHeader(eq("Access-Control-Allow-Credentials"), eq("false"));
            verify(response).setHeader(eq("Access-Control-Max-Age"), eq("60"));
            verify(response).setHeader(eq("Access-Control-Allow-Headers"), eq("Content-Type, Accept"));
        });
    }

    @Test
    public void leggerIkkePaHeadersOmOriginIkkeMatcher() {
        HttpServletRequest request = getRequestMock("GET", "www.nav.no");
        HttpServletResponse response = getResponseMock();
        FilterChain chain = getFilterChainMock();

        Map<String, String> corsSettings = corsSettings(
                Pair.of(CORS_ALLOWED_ORIGINS, ".localhost.no")
        );

        withCorsSettings(corsSettings, () -> {
            NavCorsFilter filter = new NavCorsFilter();

            filter.doFilter(request, response, chain);

            verify(response, times(0)).setHeader(anyString(), anyString());
        });
    }

    private void assertValidOrigin(String origin, String... validSubDomains) {
        assertThat(NavCorsFilter.validOrigin(origin, asList(validSubDomains))).isTrue();
    }

    private void assertInvalidOrigin(String origin, String... validSubDomains) {
        assertThat(NavCorsFilter.validOrigin(origin, validSubDomains != null ? asList(validSubDomains) : Collections.emptyList())).isFalse();
    }

    private static CorsHeader createCorsOrigin() {
        return new CorsHeader(
                "Access-Control-Allow-Origin",
                CORS_ALLOWED_ORIGINS,
                Collections.emptyList(),
                NavCorsFilter::validerAllowOrigin
        );
    }

    private static HttpServletRequest getRequestMock(String method, String origin) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getHeader(eq(ORIGIN))).thenReturn(origin);
        return request;
    }

    private static HttpServletResponse getResponseMock() {
        return mock(HttpServletResponse.class);
    }

    @SneakyThrows
    private static FilterChain getFilterChainMock() {
        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(any(), any());
        return chain;
    }

    private static Map<String, String> corsSettings(Pair<String, String>... entries) {
        HashMap<String, String> settings = new HashMap<>();
        Stream.of(entries).forEach((entry) -> settings.put(entry.getFirst(), entry.getSecond()));
        return settings;
    }

    @SneakyThrows
    private static void withCorsSettings(Map<String, String> corsEnv, UnsafeRunnable fn) {
        recreateCorsHeaders(corsEnv);
        fn.run();
        recreateCorsHeaders(emptyMap());
    }

    @SneakyThrows
    private static void recreateCorsHeaders(Map<String, String> corsEnv) {
        corsEnv.forEach(System::setProperty);
        List<Field> alleHeaders = Stream.of(NavCorsFilter.class.getDeclaredFields())
                .filter((field) -> field.getType().isAssignableFrom(CorsHeader.class))
                .collect(Collectors.toList());

        alleHeaders.forEach(NavCorsFilterTest::recreateCorsHeader);

        Field headerList = NavCorsFilter.class.getDeclaredField("CORS_HEADER_LIST");
        makeAccessible(headerList);
        List<CorsHeader> nyHeaderListe = alleHeaders
                .stream()
                .map(sneakyFunction((field) -> (CorsHeader) field.get(null)))
                .collect(Collectors.toList());
        headerList.set(null, nyHeaderListe);
    }

    @SneakyThrows
    private static void recreateCorsHeader(Field field) {
        makeAccessible(field);

        CorsHeader corsHeader = (CorsHeader) field.get(null);
        Object newHeader = corsHeader
                .getClass()
                .getConstructor(String.class, String.class, List.class, Function.class)
                .newInstance(
                        corsHeader.header,
                        corsHeader.environmentPropery,
                        corsHeader.defaultValue,
                        corsHeader.validator
                );

        field.set(null, newHeader);
    }

    @SneakyThrows
    private static void makeAccessible(Field field) {
        field.setAccessible(true);
        Field modifierField = Field.class.getDeclaredField("modifiers");
        modifierField.setAccessible(true);
        modifierField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}