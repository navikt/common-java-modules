package no.nav.common.auth.utils;

import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CookieUtilsTest {

    @Test
    public void getCookie_shouldReturnCorrectCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
           new Cookie("test", "hello")
        });

        assertTrue(CookieUtils.getCookie("test", request).isPresent());
    }

    @Test
    public void getCookie_shouldReturnEmptyIfRequestHasNoCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        assertFalse(CookieUtils.getCookie("test", request).isPresent());
    }

    @Test
    public void getCookie_shouldReturnEmptyIfNoCookiesMatch() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("test", "hello")
        });

        assertFalse(CookieUtils.getCookie("test2", request).isPresent());
    }

    @Test
    public void getCookie_shouldReturnFirstMatching() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("test", "hello"),
                new Cookie("test", "world")
        });

        Optional<Cookie> cookie = CookieUtils.getCookie("test", request);
        assertTrue(cookie.isPresent());
        assertEquals("hello", cookie.get().getValue());
    }

}
