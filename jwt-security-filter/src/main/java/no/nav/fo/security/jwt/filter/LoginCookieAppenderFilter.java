package no.nav.fo.security.jwt.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fo.security.jwt.context.SubjectHandler;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Provider
@LoginCookieAppender
/**
 * Dette er et filter som setter cookie som browser skal bruke for å autentisere brukeren.
 */
public class LoginCookieAppenderFilter implements ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginCookieAppenderFilter.class);
    private static final String host = getFQDN();

    private static String getFQDN() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.error("Could not resolve host", e);
            return "unknown_host";
        }
    }


    @Context
    HttpServletResponse resp;

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Cookie cookie = new Cookie("Authentication", SubjectHandler.getSubjectHandler().getInternSsoToken());
        cookie.setDomain(host);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        resp.addCookie(cookie);

        log.info("Added autentication-cookie");
    }
}
