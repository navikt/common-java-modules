package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.common.i18n.Exception;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CXFMaskTokenLoggingInInterceptorTest {

    @Test
    public void skalFjerneCookieFraHttpHeaders() throws java.lang.Exception {
        Logger logger = mock(Logger.class);
        CXFMaskTokenLoggingInInterceptor loggingInInterceptor =  new CXFMaskTokenLoggingInInterceptor();
        ArgumentCaptor<LogRecord> captor = ArgumentCaptor.forClass(LogRecord.class);
        when(logger.isLoggable(any())).thenReturn(true);
        Message message = createMessage();
        assertTrue(message.toString().contains("Cookie"));
        loggingInInterceptor.logging(logger, message);
        verify(logger).log(captor.capture());
        assertFalse(captor.getValue().getMessage().contains("Cookie"));
    }

    public Message createMessage() throws Exception {
        Exchange exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, null);
        Message message = new MessageImpl();
        message.setExchange(exchange);

        TreeMap<String, List<String>> headers = new TreeMap<>();
        List<String> cookieHeader = new ArrayList<>();
        cookieHeader.add("ID_Token=ekjfbsd");
        cookieHeader.add("refresh_token=ekjfdbsd");
        List<String> acceptHeader = new ArrayList<>();
        acceptHeader.add("text/html");
        headers.put("Cookie", cookieHeader);
        headers.put("Accept", acceptHeader);
        message.put(Message.PROTOCOL_HEADERS, headers);

        return message;
    }

}