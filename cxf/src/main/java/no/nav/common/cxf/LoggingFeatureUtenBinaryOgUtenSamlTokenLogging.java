package no.nav.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.AbstractLoggingInterceptor;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;


public class LoggingFeatureUtenBinaryOgUtenSamlTokenLogging extends AbstractFeature {

    private static final int DEFAULT_LIMIT = AbstractLoggingInterceptor.DEFAULT_LIMIT;
    private static final LoggingInInterceptor IN = new LoggingInInterceptor();
    private static final CXFMaskSAMLTokenLoggingOutInterceptor OUT = new CXFMaskSAMLTokenLoggingOutInterceptor(DEFAULT_LIMIT);

    static {
        IN.setLimit(DEFAULT_LIMIT);
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
