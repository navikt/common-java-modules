package no.nav.common.cxf.saml;

import no.nav.common.auth.subject.Subject;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static no.nav.common.auth.subject.SubjectHandler.withSubjectProvider;

public class CXFServletWithAuth extends CXFNonSpringServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res) {
        withSubjectProvider(
                () -> (Subject) req.getAttribute(SAMLInInterceptor.SUBJECT_REQUEST_ATTRIBUTE_NAME),
                () -> super.service(req, res)
        );
    }

}
