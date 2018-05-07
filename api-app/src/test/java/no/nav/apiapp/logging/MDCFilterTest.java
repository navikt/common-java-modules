package no.nav.apiapp.logging;

import no.nav.brukerdialog.security.context.CustomizableSubjectHandler;
import no.nav.brukerdialog.security.context.SubjectHandler;
import org.junit.Test;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

public class MDCFilterTest {

    private static final Logger LOG = getLogger(MDCFilterTest.class);

    private MDCFilter mdcFilter = new MDCFilter();

    static {
        System.setProperty(SubjectHandler.SUBJECTHANDLER_KEY, CustomizableSubjectHandler.class.getName());
    }

    @Test
    public void smoketest() throws ServletException, IOException {
        mdcFilter.doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class), (request, response) -> LOG.info("testing logging"));
    }

}