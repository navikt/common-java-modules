package no.nav.modig.security.filter;

import java.io.IOException;

import javax.security.auth.Destroyable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.nav.modig.core.context.SubjectHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Se <a href=http://confluence.adeo.no/pages/viewpage.action?pageId=54276633>Utviklerhåndbok - Sikkerhet - Ekstern Single Sign
 * On</a> for når og hvordan dette filteret skal brukes.
 */
public class OpenAMLoginFilter extends OncePerRequestFilter {
    static final String EKSTERN_SSO_COOKIE_KEY = "no.nav.modig.presentation.security.session.eksternSsoCookieName";
    private static final String DEFAULT_EKSTERN_SSO_COOKIE_NAME = "nav-esso";
    static final String SECLEVEL_HEADER_KEY = "no.nav.modig.security.filter.secLevelHeaderKey";
    private static final String DEFAULT_SEC_LEVEL_HEADER_KEY = "X-NAV-SecLevel";

    private static final Logger logger = LoggerFactory.getLogger(OpenAMLoginFilter.class);

    String eksternSsoCookieName;
    String secLevelHeaderKey;
    private SubjectHandler subjectHandler;
    private OpenAMService openAMService;

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        eksternSsoCookieName = resolveProperty(EKSTERN_SSO_COOKIE_KEY, DEFAULT_EKSTERN_SSO_COOKIE_NAME);
        secLevelHeaderKey = resolveProperty(SECLEVEL_HEADER_KEY, DEFAULT_SEC_LEVEL_HEADER_KEY);
        subjectHandler = SubjectHandler.getSubjectHandler();
        openAMService = newOpenAMService();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        logger.debug("Entering filter to validate session against the authenticated user.");
        String requestEksternSsoToken = getRequestEksternSsoToken(request);
        String loggedinUid = subjectHandler.getUid();
        String loggedinEksternSsoToken = subjectHandler.getEksternSsoToken();
        boolean allowAccess;
        boolean requestUserPrincipalIsDestroyed = false;
        logger.debug("SubjectHandler.uid: " + loggedinUid);
        logger.debug("UserPrincipal: " + request.getUserPrincipal());

        if(request.getUserPrincipal() != null) {
        	logger.debug("UserPrincipal.name: " + request.getUserPrincipal().getName());
        	logger.debug("UserPrincipal.class: " + request.getUserPrincipal().getClass());
        	requestUserPrincipalIsDestroyed = ((Destroyable) request.getUserPrincipal()).isDestroyed();
        	logger.debug("UserPrincipal on request is destroyed="+ requestUserPrincipalIsDestroyed);
        }
        
        if(requestUserPrincipalIsDestroyed) {
        	logger.debug("UserPrincipal exists on request but has been destroyed. Logging out to clear userprincipal before new login will be performed.");
        	request.logout();
        	request.getSession().invalidate();
        }

        if (loggedinUid == null) {
            logger.debug("User isn't logged into the container.");
            allowAccess = handleNoExistingLogin(request, response, requestEksternSsoToken);
        } else {
            logger.debug("User is already logged into the container. Validating cookie and session.");
            allowAccess = handleExistingLogin(request, response, requestEksternSsoToken, loggedinUid, loggedinEksternSsoToken);
        }
        if (allowAccess) {
            if (subjectHandler.getUid() != null) {
                request.getSession(); // Ensure session exists to map requests to logged in user in container
            }

            filterChain.doFilter(request, response);
        }
    }

    private boolean handleNoExistingLogin(HttpServletRequest request, HttpServletResponse response, String requestEksternSsoToken) throws IOException {

        boolean allowAccess = true;

        if (requestEksternSsoToken == null) {
            logger.debug("No SSO cookie found. Assuming this is a unprotected resource, do nothing.");
        } else if (!openAMService.isTokenValid(requestEksternSsoToken)) {
            logger.debug("SSO cookie was expired. Assuming this is a unprotected resource. Removing cookie, passing request to chain.");
            removeSsoToken(request, response);
        } else {
            allowAccess = login(request, requestEksternSsoToken, response);
        }
        return allowAccess;
    }

    private boolean handleExistingLogin(HttpServletRequest request, HttpServletResponse response, String requestEksternSsoToken, String loggedinUid, String loggedinEksternSsoToken) throws ServletException, IOException {

        boolean allowAccess = true;

        if (requestEksternSsoToken == null) {
            logger.debug("User is logged into the container, but could not find expected cookie \"{}\" with External SSO Token", eksternSsoCookieName);
            logger.debug("Logging the user out and invalidating httpsession.");

            request.logout();
            request.getSession().invalidate();

        } else if (!requestEksternSsoToken.equals(loggedinEksternSsoToken)){
            logger.debug("The loggedin External SSO session ({}) differs from the External SSO session ({}) in the request. Logging in", loggedinEksternSsoToken, requestEksternSsoToken);
            request.logout();
            allowAccess = login(request, requestEksternSsoToken, response);

            if (allowAccess) {
                String uidAfterNewLogin = subjectHandler.getUid();
                if (!loggedinUid.equals(uidAfterNewLogin)) {
                    logger.debug("The UID in the new External SSO session ({}) differs from the UID i the old External SSO session ({}). Invalidating the HttpSession", uidAfterNewLogin, loggedinUid);

                    //we need to invalidate the session,
                    request.logout();
                    request.getSession().invalidate();

                    //but this also logs out the new user, hence we need to re-login
                    allowAccess = login(request, requestEksternSsoToken, response);
                }
            }

        } else {
            String secLevelHeader = request.getHeader(secLevelHeaderKey);

            if(secLevelHeader != null && Integer.parseInt(secLevelHeader) > subjectHandler.getAuthenticationLevel()){
                logger.debug("The logged in authentication level does not match header from reverse proxy, assuming step-up and performing re-login");

                request.logout();
                request.getSession().invalidate();

                allowAccess = login(request, requestEksternSsoToken, response);

                //WORKAROUD FOR MODFEIL-666, to be removed when OpenAM manages to set sec-level-hdr correctly
            } else if(secLevelHeader != null
                    && Integer.parseInt(secLevelHeader) != 4
                    && Integer.parseInt(secLevelHeader) == subjectHandler.getAuthenticationLevel()
                    ){
                
                logger.debug("The logged in authentication level does not match header from reverse proxy, assuming step-up and performing re-login");

                request.logout();
                request.getSession().invalidate();

                allowAccess = login(request, requestEksternSsoToken, response);

            } else {
                logger.debug("SSO cookie and session validation succeeded.");
            }
        }
        return allowAccess;
    }

    protected boolean login(HttpServletRequest request, String requestEksternSsoToken, HttpServletResponse response) throws IOException {
        try {
            logger.debug("Logging in with External SSO Token \"{}={}\".", eksternSsoCookieName, requestEksternSsoToken);
            // JBOSS EAP7 does not accept null password, Changed to empty string.
            request.login(requestEksternSsoToken, "");
            containerSpecificLogin(request);
            return true;
        } catch (ServletException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            logger.debug("Login failed.", e);
            return false;
        }
    }

    protected void containerSpecificLogin(HttpServletRequest request) {
    }

    protected SubjectHandler getSubjectHandler() {
        return subjectHandler;
    }

    private String getRequestEksternSsoToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(eksternSsoCookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void removeSsoToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(eksternSsoCookieName)) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setDomain(".nav.no");
                    response.addCookie(cookie);
                }
            }
        }
    }

    protected OpenAMService newOpenAMService() {
        return new OpenAMService();
    }

    private static String resolveProperty(String key, String defaultValue) {
        String value;
        if (System.getProperty(key) != null) {
            value = System.getProperty(key);
            logger.debug("Setting " + key + "={} from System.properties", value);
        } else {
            value = defaultValue;
            logger.debug("Setting " + key + "={} from application default", value);
        }
        return value;
    }

}
