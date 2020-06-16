package no.nav.common.cxf;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CXFClientInvocationHandlerTest {

    private StsConfig stsConfig = StsConfig.builder().url("https://dummy.sts.url").username("dummyUsername").password("dummyPassword").build();

    @Test
    public void shouldReturnTheExceptionTypeTheConsumerIsExpecting() throws Throwable {
        CXFClient client = new CXFClient<>(DummyWebService.class).configureStsForSubject(stsConfig);
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
        CXFClient client = new CXFClient<>(DummyWebService.class).configureStsForSubject(stsConfig);
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
