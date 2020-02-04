package no.nav.apiapp.auth;

import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticationFilterTest {

    private OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(Collections.emptyList(), Arrays.asList("/public.*"));

    @Test
    public void isPublic() {
        authenticationFilter.init(config("/abc"));
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/abc/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/abc/public/selftest"))).isTrue();

        authenticationFilter.init(config("/"));
        assertThat(authenticationFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isTrue();

        authenticationFilter.init(config(null));
        assertThat(authenticationFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isTrue();
    }

    private HttpServletRequest request(String requestPath) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn(requestPath);
        return httpServletRequest;
    }

    private FilterConfig config(String contextPath) {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        return filterConfig;
    }

}