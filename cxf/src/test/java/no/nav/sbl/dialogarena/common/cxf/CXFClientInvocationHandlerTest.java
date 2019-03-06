package no.nav.sbl.dialogarena.common.cxf;

import no.nav.sbl.util.EnvironmentUtils;
import no.nav.sbl.util.PropertyUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CXFClientInvocationHandlerTest {

    @Before
    public void setup() {
        PropertyUtils.setProperty("no.nav.modig.security.sts.url", "https://dummy.sts.url", EnvironmentUtils.Type.PUBLIC);
        PropertyUtils.setProperty("no.nav.modig.security.systemuser.username", "dummyUsername", EnvironmentUtils.Type.SECRET);
        PropertyUtils.setProperty("no.nav.modig.security.systemuser.password", "dummyPassword", EnvironmentUtils.Type.SECRET);
    }

    @Test
    public void shouldReturnTheExceptionTypeTheConsumerIsExpecting() throws Throwable {
        CXFClient client = new CXFClient<>(DummyWebService.class).configureStsForSubject();
        CXFClientInvocationHandler cxfClientInvocationHandler = new CXFClientInvocationHandler(client);

        DummyWebServiceException expectedException = new DummyWebServiceException("Expected exception");
        InvocationTargetException expectedExceptionWrapped = new InvocationTargetException(expectedException);

        InvocationHandler invocationHandlerMock = mock(InvocationHandler.class);
        when(invocationHandlerMock.invoke(any(), any(), any())).thenThrow(expectedExceptionWrapped);
        ReflectionTestUtils.setField(cxfClientInvocationHandler, "invocationHandler", invocationHandlerMock);

        Exception catchedException = null;
        DummyWebService dummyWebService = new DefaultDummyWebService();
        Method methodOnDummyWebService = dummyWebService.getClass().getMethod("webServiceCall");
        Object[] noObjects = new Object[0];
        try {
            cxfClientInvocationHandler.invoke(dummyWebService, methodOnDummyWebService, noObjects);

        } catch (Exception e) {
            catchedException = e;
        }
        assertThat(catchedException, is(notNullValue()));
        assertThat(catchedException, is(instanceOf(expectedException.getClass())));
        assertThat(catchedException.getMessage(), is(expectedException.getMessage()));
    }

    @Test
    public void shouldNotAffectExceptionsNotWrappedInInvocationTargetException() throws Throwable {
        CXFClient client = new CXFClient<>(DummyWebService.class).configureStsForSubject();
        CXFClientInvocationHandler cxfClientInvocationHandler = new CXFClientInvocationHandler(client);

        RuntimeException expectedException = new RuntimeException("Expected exception");

        InvocationHandler invocationHandlerMock = mock(InvocationHandler.class);
        when(invocationHandlerMock.invoke(any(), any(), any())).thenThrow(expectedException);
        ReflectionTestUtils.setField(cxfClientInvocationHandler, "invocationHandler", invocationHandlerMock);

        Exception catchedException = null;
        DummyWebService dummyWebService = new DefaultDummyWebService();
        Method methodOnDummyWebService = dummyWebService.getClass().getMethod("webServiceCall");
        Object[] noObjects = new Object[0];
        try {
            cxfClientInvocationHandler.invoke(dummyWebService, methodOnDummyWebService, noObjects);

        } catch (Exception e) {
            catchedException = e;
        }
        assertThat(catchedException, is(notNullValue()));
        assertThat(catchedException, is(instanceOf(expectedException.getClass())));
        assertThat(catchedException.getMessage(), is(expectedException.getMessage()));
    }

    private interface DummyWebService {
        String webServiceCall() throws DummyWebServiceException;
    }

    private class DefaultDummyWebService implements DummyWebService {
        public String webServiceCall() throws DummyWebServiceException {
            return "dummy result";
        }
    }

    private class DummyWebServiceException extends RuntimeException {
        public DummyWebServiceException(String message) {
            super(message);
        }
    }

}
