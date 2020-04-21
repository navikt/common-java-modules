package no.nav.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.*;

public class LoggingFeatureUtenTokenLogging extends AbstractFeature{
    private static final int DEFAULT_LIMIT = AbstractLoggingInterceptor.DEFAULT_LIMIT;
    private static final CXFMaskTokenLoggingInInterceptor IN = new CXFMaskTokenLoggingInInterceptor(DEFAULT_LIMIT);
    private static final LoggingOutInterceptor OUT = new LoggingOutInterceptor(DEFAULT_LIMIT);
    static {
        IN.addAfter(AttachmentInInterceptor.class.getName());
        OUT.addAfter(AttachmentOutInterceptor.class.getName());
    }

    public LoggingFeatureUtenTokenLogging() {
    }

    public void setMaskerTokenIHeader(boolean maskerTokenIHeader) {
        IN.setMaskerTokenLogging(maskerTokenIHeader);
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(IN);
            provider.getInFaultInterceptors().add(IN);
            provider.getOutInterceptors().add(OUT);
            provider.getOutFaultInterceptors().add(OUT);
    }
}
