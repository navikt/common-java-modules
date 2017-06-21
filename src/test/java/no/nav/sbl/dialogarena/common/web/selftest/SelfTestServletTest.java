package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.types.Pingable;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static no.nav.sbl.dialogarena.common.web.selftest.AbstractSelfTestBaseServlet.STATUS_ERROR;
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
    private SelfTestBaseServlet baseServlet;
    private ServletTester tester;
    private SelfTestJsonBaseServlet jsonBaseServlet;

    @Before
    public void setUp() throws Exception {
        baseServlet = createBaseServlet();
        jsonBaseServlet = createJsonBaseServlet();

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<String, String[]>());
        when(mockResponse.getWriter()).thenReturn(createMockedPrintWriter());


        tester = new ServletTester();
        tester.setContextPath("/internal");
        tester.addServlet(baseServlet.getClass(), "/selftest");
        tester.addServlet(jsonBaseServlet.getClass(), "/selftest.json");
        tester.start();
    }

    @After
    public void tearDown() throws Exception {
        tester.stop();
    }

    @Test
    public void testSelfTestBase() throws ServletException, IOException {
        baseServlet.doGet(mockRequest, mockResponse);

        assertNotNull(baseServlet);
        assertThat(baseServlet.getApplicationName(), is("TestApp"));
        assertThat(baseServlet.getApplicationVersion(), is("unknown version"));
        assertTrue(baseServlet.getHost().endsWith(".devillo.no"));
        assertThat(baseServlet.getAggregertStatus(), is(STATUS_ERROR));
        assertThat(baseServlet.getPingables().size(), is(3));
    }

    private PrintWriter createMockedPrintWriter() {
        return new PrintWriter(new CharArrayWriter());
    }

    private SelfTestBaseServlet createBaseServlet() {
        return new SelfTestBaseServlet() {
            @Override
            protected String getApplicationName() {
                return "TestApp";
            }

            @Override
            protected Collection<? extends Pingable> getPingables() {
                return asList(
                        createPingable(Ping.lyktes("A", "beskrivelse")),
                        createPingable(Ping.lyktes("B", "beskrivelse")),
                        createPingable(Ping.feilet("C", "beskrivelse", true, new IllegalArgumentException("Cfeil")))
                );
            }
        };
    }

    private SelfTestJsonBaseServlet createJsonBaseServlet() {
        return new SelfTestJsonBaseServlet() {
            @Override
            protected String getApplicationName() {
                return "TEST APPLICATION";
            }

            @Override
            protected Collection<? extends Pingable> getPingables() {
                return asList(
                        createPingable(Ping.lyktes("A", "beskrivelse")),
                        createPingable(Ping.feilet("B", "beskrivelse", true, new IllegalArgumentException("BB")))
                );
            }
        };
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
