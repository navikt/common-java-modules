package no.nav.apiapp.soap;

import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapFaultListener implements FaultListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoapFaultListener.class);

    @Override
    public boolean faultOccurred(Exception exception, String description, Message message) {
        LOGGER.error(description, exception);
        return true;
    }

}
