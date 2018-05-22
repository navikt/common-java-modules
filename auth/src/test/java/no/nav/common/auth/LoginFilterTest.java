package no.nav.common.auth;

import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoginFilterTest {

    private LoginProvider loginProvider = mock(LoginProvider.class);
    private LoginFilter loginFilter = new LoginFilter(Arrays.asList(loginProvider), Arrays.asList("/public.*"));
    
    @Test
    public void isPublic() throws ServletException {
        loginFilter.init(config("/abc"));
        assertThat(loginFilter.isPublic(request("/public/selftest"))).isFalse();
        assertThat(loginFilter.isPublic(request("/abc/api/def/public/selftest"))).isFalse();
        assertThat(loginFilter.isPublic(request("/abc/public/selftest"))).isTrue();

        loginFilter.init(config("/"));
        assertThat(loginFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(loginFilter.isPublic(request("/public/selftest"))).isTrue();

        loginFilter.init(config(null));
        assertThat(loginFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(loginFilter.isPublic(request("/public/selftest"))).isTrue();
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