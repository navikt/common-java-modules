package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;
import java.util.logging.Logger;

public class CXFMaskTokenLoggingInInterceptor extends LoggingInInterceptor{
    private boolean maskerToken = true;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CXFMaskTokenLoggingInInterceptor.class);


    public CXFMaskTokenLoggingInInterceptor() {
        super();
    }

    public CXFMaskTokenLoggingInInterceptor(int limit) {
        super(limit);
    }

    public void setMaskerTokenLogging(boolean maskerTokenLogging) {
        this.maskerToken = maskerTokenLogging;
    }


    @Override
    protected void logging(Logger logger, Message message) throws Fault {
        Message maskedMessage = new MessageImpl(message);
        // Skal kun endre headers i maskedMessage, ikke i message
        TreeMap headers = (TreeMap) ((TreeMap)maskedMessage.get(Message.PROTOCOL_HEADERS)).clone();
        if (headers != null && maskerToken) {
                if(headers.containsKey("Cookie")) {
                    headers.remove("Cookie");
                    maskedMessage.remove(Message.PROTOCOL_HEADERS);
                    maskedMessage.put(Message.PROTOCOL_HEADERS, headers);
                }
        }
        super.logging(logger, maskedMessage);
    }
}
