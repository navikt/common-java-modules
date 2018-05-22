package no.nav.sbl.dialogarena.common.cxf.saml;

import no.nav.common.auth.Subject;
import no.nav.sbl.util.fn.UnsafeSupplier;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.common.auth.SubjectHandler.withSubjectProvider;

public class CXFServletWithAuth extends CXFNonSpringServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        withSubjectProvider(
                () -> (Subject) req.getAttribute(SAMLInInterceptor.SUBJECT_REQUEST_ATTRIBUTE_NAME),
                () -> super.service(req, res)
        );
    }

}
