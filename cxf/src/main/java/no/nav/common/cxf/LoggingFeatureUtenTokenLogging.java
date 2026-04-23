package no.nav.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.AbstractLoggingInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;

public class LoggingFeatureUtenTokenLogging extends AbstractFeature {

    private static final int DEFAULT_LIMIT = AbstractLoggingInterceptor.DEFAULT_LIMIT;
    private static final CXFMaskTokenLoggingInInterceptor IN = new CXFMaskTokenLoggingInInterceptor(DEFAULT_LIMIT);
    private static final LoggingOutInterceptor OUT = new LoggingOutInterceptor();

    static {
        OUT.setLimit(DEFAULT_LIMIT);
        IN.addAfter(AttachmentInInterceptor.class.getName());
        OUT.addAfter(AttachmentOutInterceptor.class.getName());
    }

    public LoggingFeatureUtenTokenLogging() {}

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
