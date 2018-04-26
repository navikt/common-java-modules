package no.nav.modig.security.filter;

//import ch.qos.logback.classic.Level;
import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;

import no.nav.modig.core.domain.SluttBruker;
//import no.nav.modig.core.test.LogSniffer;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
//import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.security.auth.Subject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OpenAMLoginFilterTest {
    private static final String FAIL_THIS_LOGIN = "Fail this login";
    public static final String ANOTHER_USER = "01015200002";
    public static final String A_USER = "01015245464";
    private static final List<String> A_USERS_TOKENS = Arrays.asList("01015245464.1cwNQXiiUlH9v1lnM47CZ-SFacKmszZ2BeI.*AAJTSQACMDE.*", "01015245464.2cwNQXiiUlH9v1lnM47CZ-SFacKmszZ2BeI.*AAJTSQACMDE.*");
    private static final List<String> ANOTHER_USERS_TOKENS = Arrays.asList("01015200002.1cwNQXiiUlH9v1lnM47CZ-SFacKmszZ2BeI.*AAJTSQACMDE.*");
    private static final Map<String, String> TOKEN_USER_MAP = new HashMap<>();
    static {
        for (String token : A_USERS_TOKENS) {
            TOKEN_USER_MAP.put(token, A_USER);
        }
        for (String token : ANOTHER_USERS_TOKENS) {
            TOKEN_USER_MAP.put(token, ANOTHER_USER);
        }
    }

//    @Rule
//    public LogSniffer logSniffer = new LogSniffer(Level.DEBUG);
	@Mock
	private OpenAMService mockService;
	@Mock
	private FilterChain filterChain;

    private OpenAMLoginFilter filter;
    private MyMockedHttpServletRequest request;
    private MockHttpServletResponse response;
    private StaticSubjectHandler subjectHandler;

    @Before
    public void setup() throws ServletException {
        System.setProperty(SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
		initMocks(this);

		filter = new OpenAMLoginFilter() {
			@Override
			protected OpenAMService newOpenAMService() {
				return mockService;
			}
		};
        filter.initFilterBean();
        subjectHandler = (StaticSubjectHandler) filter.getSubjectHandler();

        request = new MyMockedHttpServletRequest("GET", "/");
        request.setSubjectHandler(subjectHandler);

        response = new MockHttpServletResponse();
		when(mockService.isTokenValid(anyString())).thenReturn(true);
    }

    @After
    public void cleanup() {
        subjectHandler.setSubject(null);
    }

    @Test
    public void shouldLogoutAndInvalidateSessionWhenNoOpenAmSessionTokenAlreadyLoggedIn() throws ServletException, IOException {
        request.login(A_USERS_TOKENS.get(0), null);
        assertThat(subjectHandler.getSubject(), notNullValue());
        filter.doFilter(request, response, filterChain);
        assertThat(subjectHandler.getUid(), nullValue());
    }

    @Test
    public void shouldNotDoAnythingWhenNotLoggedInAndNoOpenAmToken() throws Exception {
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

	@Test
	public void shouldRemoveTokenWhenNotLoggedInAndOpenAmTokenIsExpired() throws Exception{
		Cookie ssoTokenCookie = new Cookie(filter.eksternSsoCookieName, FAIL_THIS_LOGIN);
		request.setCookies(new Cookie[]{ssoTokenCookie});
		when(mockService.isTokenValid(anyString())).thenReturn(false);

		filter.doFilter(request, response, filterChain);
		verify(filterChain).doFilter(request, response);

		assertThat(asList(response.getCookies()), hasItem(both(maxAge(0)).and(name(ssoTokenCookie.getName()))));
	}


    @Test
    public void shouldLoginNewOpenAmSession() throws Exception {
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(0)) });

        filter.doFilter(request, response, filterChain);

        assertThat(subjectHandler.getEksternSsoToken(), is(A_USERS_TOKENS.get(0)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldNotDoAnythingWhenLoggedInAndOpenAmTokensMatch() throws Exception {
        request.login(A_USERS_TOKENS.get(0), null);
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(0)) });

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldLoginAndKeepHttpSessionWhenNewOpenAmSessionWithSameUser() throws Exception {
        request.login(A_USERS_TOKENS.get(0), null);
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(1)) });
        request.setSession(new MockHttpSession());

        filter.doFilter(request, response, filterChain);

        assertThat(subjectHandler.getEksternSsoToken(), is(A_USERS_TOKENS.get(1)));
        HttpSession session = request.getSession(false);
        assertThat(session, notNullValue());
        assertThat(session.isNew(), is(false));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldLoginAndInvalidateHttpSessionWhenNewOpenAmSessionWithAnotherUid() throws Exception {
        request.login(A_USERS_TOKENS.get(0), null);
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, ANOTHER_USERS_TOKENS.get(0)) });
        HttpSession initialHttpSession = new MockHttpSession();
        request.setSession(initialHttpSession);

        filter.doFilter(request, response, filterChain);

        assertThat(subjectHandler.getEksternSsoToken(), is(ANOTHER_USERS_TOKENS.get(0)));
        assertThat(request.getSession(false), is(not(initialHttpSession)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldSend403_FORBIDDEN_IfLoginFails() throws Exception {
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, FAIL_THIS_LOGIN) });

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    public void aPreviousFailedLoginShouldNotBlockAccessAfterASuccessfullLogin() throws Exception {
        // A failing login attempt
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, FAIL_THIS_LOGIN) });

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));

        // A successfull login attempt
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(0)) });

        filter.doFilter(request, response, filterChain);

        assertThat(subjectHandler.getEksternSsoToken(), is(A_USERS_TOKENS.get(0)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldStepUpBasedOnSecLevelHeader() throws Exception {
        int authenticationLevelInOpenAMSession = 3;
        request.setMockAuthenticationLevelForOpenAMSession(authenticationLevelInOpenAMSession);
        request.addHeader("X-NAV-SecLevel", authenticationLevelInOpenAMSession);
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(0)) });

        filter.doFilter(request, response, filterChain);
        assertThat(subjectHandler.getEksternSsoToken(), is(A_USERS_TOKENS.get(0)));
        assertThat(subjectHandler.getAuthenticationLevel(), is(authenticationLevelInOpenAMSession));
        verify(filterChain).doFilter(request, response);

        //adding new request to clear headers
        request = new MyMockedHttpServletRequest("GET", "/");
        request.setSubjectHandler(subjectHandler);

        authenticationLevelInOpenAMSession = 4;
        request.setMockAuthenticationLevelForOpenAMSession(authenticationLevelInOpenAMSession);
        request.addHeader("X-NAV-SecLevel", authenticationLevelInOpenAMSession);
        request.setCookies(new Cookie[] { new Cookie(filter.eksternSsoCookieName, A_USERS_TOKENS.get(0)) });

        filter.doFilter(request, response, filterChain);
        assertThat(subjectHandler.getEksternSsoToken(), is(A_USERS_TOKENS.get(0)));
        assertThat(subjectHandler.getAuthenticationLevel(), is(authenticationLevelInOpenAMSession));

        //new request, single invocation only?
        verify(filterChain).doFilter(request, response);
    }

    private class MyMockedHttpServletRequest extends MockHttpServletRequest {
        private StaticSubjectHandler subjectHandler;
        private int authenticationLevel = 4;

        private MyMockedHttpServletRequest(String method, String requestURI) {
            super(method, requestURI);
        }

        @Override
        public void login(final String username, String password) throws ServletException {
            if (getUserPrincipal() != null || getRemoteUser() != null || getAuthType() != null) {
                throw new ServletException("A non-null caller identity had already been established (prior to the call to login");
            }

            if (FAIL_THIS_LOGIN.equals(username)) {
                throw new ServletException("Failing this login as requested");
            }

            SluttBruker userPrincipal = SluttBruker.eksternBruker(TOKEN_USER_MAP.get(username));
            Subject subject = subjectHandler.getSubject();
            if (subject == null) {
                subject = new Subject();
                subjectHandler.setSubject(subject);
            }
            subject.getPrincipals().add(userPrincipal);
            subject.getPublicCredentials().add(new OpenAmTokenCredential(username));
            subject.getPublicCredentials().add(new AuthenticationLevelCredential(authenticationLevel));

            setUserPrincipal(userPrincipal);
            setRemoteUser(username);
            setAuthType(HttpServletRequest.BASIC_AUTH);// TODO: (OUH) Hva skal egentlig inn her?
        }

        @Override
        public void logout() {
            setUserPrincipal(null);
            setRemoteUser(null);
            setAuthType(null);

            Subject subject = subjectHandler.getSubject();
            subject.getPrincipals().removeAll(subject.getPrincipals(SluttBruker.class));
            subject.getPublicCredentials().removeAll(subject.getPublicCredentials(OpenAmTokenCredential.class));
            subject.getPublicCredentials().removeAll(subject.getPublicCredentials(AuthenticationLevelCredential.class));
            subjectHandler.setSubject(subject);
        }

        public void setSubjectHandler(StaticSubjectHandler subjectHandler) {
            this.subjectHandler = subjectHandler;
        }

        public void setMockAuthenticationLevelForOpenAMSession(int authenticationLevel) {
            this.authenticationLevel = authenticationLevel;
        }
    }

	private Matcher<Cookie> maxAge(final int maxAge) {
		return new TypeSafeMatcher<Cookie>() {
			@Override
			protected boolean matchesSafely(Cookie cookie) {
				return cookie.getMaxAge() == maxAge;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("with max age " + maxAge);
			}
		};
	}

	private Matcher<Cookie> name(final String name) {
		return new TypeSafeMatcher<Cookie>() {
			@Override
			protected boolean matchesSafely(Cookie cookie) {
				return cookie.getName().equals(name);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("with max name " + name);
			}
		};
	}


}
