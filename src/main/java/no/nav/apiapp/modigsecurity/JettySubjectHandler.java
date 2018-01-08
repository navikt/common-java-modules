package no.nav.apiapp.modigsecurity;

import no.nav.modig.core.context.SubjectHandler;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;

/**
 * kopiert ut fra modig-security mens vi venter på at noen får ut fingeren http://stash.devillo.no/projects/MODIG/repos/modig-security/pull-requests/5/overview
 */

public class JettySubjectHandler extends SubjectHandler {

    public JettySubjectHandler() {
    }

    public Subject getSubject() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            throw new RuntimeException(
                    "Ingen request funnet på RequestContextHolder. \n" +
                            "JettySubjectHandler krever at request holdes av Spring. \n" +
                            "Benytt RequestContextListener eller RequestContextFilter til å ta vare på request. \n" +
                            "Disse konfigureres opp i web.xml"
            );
        } else {
            Request request = (Request) servletRequestAttributes.getRequest();
            Authentication authentication = request.getAuthentication();
            return authentication instanceof Authentication.User ? ((Authentication.User)authentication).getUserIdentity().getSubject() : null;
        }
    }
}