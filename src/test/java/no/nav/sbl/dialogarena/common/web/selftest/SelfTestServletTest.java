package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.types.Pingable;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelfTestServletTest {

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private SelfTestBaseServlet servlet;
    private ServletTester tester;

    @Before
    public void setUp() throws Exception {
        servlet = new SelfTestBaseServlet() {
            @Override
            protected String getApplicationName() {
                return "TestApp";
            }

            @Override
            protected Collection<? extends Pingable> getPingables() {
                return asList(
                        createPingable(Ping.lyktes("A")),
                        createPingable(Ping.lyktes("B")),
                        createPingable(Ping.feilet("C", new IllegalArgumentException("Cfeil")))
                );
            }
        };

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());
        when(mockResponse.getWriter()).thenReturn(new PrintWriter(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        }));


        tester = new ServletTester();
        tester.setContextPath("/internal");
        tester.addServlet(servlet.getClass(), "/selftest");
        tester.start();
    }

    @After
    public void tearDown() throws Exception {
        tester.stop();
        tester = null;
        mockRequest = null;
        mockResponse = null;
        servlet = null;
    }

    @Test
    public void testSelfTest() throws ServletException, IOException {
        servlet.doGet(mockRequest, mockResponse);

        assertNotNull(servlet);
        assertThat(servlet.getApplicationName(), is("TestApp"));
        assertThat(servlet.getApplicationVersion(), is("unknown version"));
        assertTrue(servlet.getHost().endsWith(".devillo.no"));
        assertThat(servlet.getStatus(), is("ERROR"));
    }



    private Pingable createPingable(final Ping ping) {
        return new Pingable() {
            @Override
            public Ping ping() {
                return ping;
            }
        };
    }
}
