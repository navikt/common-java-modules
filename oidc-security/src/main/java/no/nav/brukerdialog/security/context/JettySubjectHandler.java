package no.nav.brukerdialog.security.context;

import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;

/**
 * SubjectHandler som kan benyttes på Jetty.
 * <p/>
 * For å benytte denne subjecthandleren på følgende system property settes:
 * JettySubjectHandler
 *
 * @see SubjectHandler
 */
public class JettySubjectHandler extends SubjectHandler {

    @Override
    public Subject getSubject() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            throw new RuntimeException("Ingen request funnet på RequestContextHolder. \n" +
                    "JettySubjectHandler krever at request holdes av Spring. \n" +
                    "Benytt RequestContextListener eller RequestContextFilter til å ta vare på request. \n" +
                    "Disse konfigureres opp i web.xml");
        }
        Request request = (Request) servletRequestAttributes.getRequest();
        Authentication authentication = request.getAuthentication();

        if (authentication instanceof Authentication.User) {
            return ((Authentication.User) authentication).getUserIdentity().getSubject();
        } else {
            return null;
        }
    }
}
