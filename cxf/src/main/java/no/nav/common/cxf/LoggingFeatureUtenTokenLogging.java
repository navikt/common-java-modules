package no.nav.common.cxf;

import org.apache.cxf.ext.logging.LoggingFeature;

public class LoggingFeatureUtenTokenLogging extends LoggingFeature {
    private final CXFMaskTokenLoggingInInterceptor maskSender = new CXFMaskTokenLoggingInInterceptor();

    public LoggingFeatureUtenTokenLogging() {
        setSender(maskSender);
    }

    public void setMaskerTokenIHeader(boolean maskerTokenIHeader) {
        maskSender.setMaskerTokenLogging(maskerTokenIHeader);
    }
}
