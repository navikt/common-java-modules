package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.*;


public class LoggingFeatureUtenBinaryOgUtenSamlTokenLogging extends AbstractFeature {

    private static final int DEFAULT_LIMIT = 64 * 1024;
    private static final LoggingInInterceptor IN = new LoggingInInterceptor(DEFAULT_LIMIT);
    private static final CXFMaskSAMLTokenLoggingOutInterceptor OUT = new CXFMaskSAMLTokenLoggingOutInterceptor(DEFAULT_LIMIT);
    static {
        IN.addAfter(AttachmentInInterceptor.class.getName());
        OUT.addAfter(AttachmentOutInterceptor.class.getName());
    }

    public LoggingFeatureUtenBinaryOgUtenSamlTokenLogging() {
    }

    public LoggingFeatureUtenBinaryOgUtenSamlTokenLogging(boolean maskerSAMLToken) {
        OUT.setMaskerSAMLToken(maskerSAMLToken);
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(IN);
            provider.getInFaultInterceptors().add(IN);
            provider.getOutInterceptors().add(OUT);
            provider.getOutFaultInterceptors().add(OUT);
    }
}
