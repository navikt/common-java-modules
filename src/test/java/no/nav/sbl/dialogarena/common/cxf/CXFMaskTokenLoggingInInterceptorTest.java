package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.common.i18n.Exception;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogUtils.class})
public class CXFMaskTokenLoggingInInterceptorTest {

    private Logger logger;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
        PowerMockito.mockStatic(LogUtils.class);
        when(LogUtils.getLogger(any())).thenReturn(logger);
    }

    @Test
    public void skalFjerneAlleHttpHeaders() throws java.lang.Exception {
        CXFMaskTokenLoggingInInterceptor loggingInInterceptor =  new CXFMaskTokenLoggingInInterceptor();
        ArgumentCaptor<LogRecord> captor = ArgumentCaptor.forClass(LogRecord.class);
        when(logger.isLoggable(any())).thenReturn(true);
        Message message = createMessage();
        loggingInInterceptor.logging(logger, message);
        verify(logger, times(1)).log(captor.capture());

    }

    private Message createMessage() throws Exception {
        Exchange exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, null);
        Message message = new MessageImpl();
        message.setExchange(exchange);
        return message;
    }

}