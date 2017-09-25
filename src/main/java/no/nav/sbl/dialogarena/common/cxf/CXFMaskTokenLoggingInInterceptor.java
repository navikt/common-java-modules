package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.message.Message;

import java.util.TreeMap;
import java.util.logging.Logger;

public class CXFMaskTokenLoggingInInterceptor extends LoggingInInterceptor{
    private boolean maskerToken = true;

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
        Message maskedMessage = message;
        TreeMap headers = (TreeMap)maskedMessage.get(Message.PROTOCOL_HEADERS);
        if (headers != null && maskerToken) {
                if(headers.containsKey("Cookie"))
                {
                    headers.remove("Cookie");
                    maskedMessage.remove(Message.PROTOCOL_HEADERS);
                    maskedMessage.put(Message.PROTOCOL_HEADERS, headers);
                }
        }
        super.logging(logger, maskedMessage);
    }


}
