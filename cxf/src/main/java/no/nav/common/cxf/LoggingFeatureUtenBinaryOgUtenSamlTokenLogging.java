package no.nav.common.cxf;

import org.apache.cxf.ext.logging.LoggingFeature;

public class LoggingFeatureUtenBinaryOgUtenSamlTokenLogging extends LoggingFeature {

    private static final int DEFAULT_LIMIT = 64 * 1024;

    private final CXFMaskSAMLTokenLoggingOutInterceptor maskSender = new CXFMaskSAMLTokenLoggingOutInterceptor();

    public LoggingFeatureUtenBinaryOgUtenSamlTokenLogging() {
        setLimit(DEFAULT_LIMIT);
        setSender(maskSender);
    }

    public LoggingFeatureUtenBinaryOgUtenSamlTokenLogging(boolean maskerSAMLToken) {
        this();
        maskSender.setMaskerSAMLToken(maskerSAMLToken);
    }
}
