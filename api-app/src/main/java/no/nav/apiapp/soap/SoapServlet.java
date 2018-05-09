package no.nav.apiapp.soap;

import no.nav.apiapp.ServletUtil;
import no.nav.sbl.dialogarena.common.cxf.CXFEndpoint;
import no.nav.sbl.dialogarena.common.cxf.saml.CXFServletWithAuth;
import org.apache.cxf.BusFactory;
import org.apache.cxf.logging.FaultListener;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collection;

public class SoapServlet extends CXFServletWithAuth {

    public static boolean soapTjenesterEksisterer(ServletContext servletContext) {
        return !getSoapTjenester(servletContext).isEmpty();
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        BusFactory.setDefaultBus(getBus());

        getSoapTjenester(servletConfig.getServletContext()).forEach((serviceBean) -> {
            CXFEndpoint cxfEndpoint = new CXFEndpoint()
                    .address(serviceBean.getClass().getAnnotation(SoapTjeneste.class).value())
                    .serviceBean(serviceBean);

            cxfEndpoint.factoryBean.setInvoker(new MethodInvokerMedFeilhandtering(serviceBean));
            cxfEndpoint.setProperty(FaultListener.class.getName(), new SoapFaultListener());

            cxfEndpoint.create();
        });
    }

    private static Collection<Object> getSoapTjenester(ServletContext servletContext) {
        return ServletUtil.getContext(servletContext).getBeansWithAnnotation(SoapTjeneste.class).values();
    }

}
