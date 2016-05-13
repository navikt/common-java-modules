package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.common.i18n.Exception;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CXFMaskSAMLTokenLoggingOutInterceptorTest {
    CXFMaskSAMLTokenLoggingOutInterceptor interceptor = new CXFMaskSAMLTokenLoggingOutInterceptor();

    @Test
    public void handleMessageSkalLeggeTilCustomLogger() throws Exception {
        Message message = createMessage();

        EndpointInfo endpoint = message.getExchange().getEndpoint().getEndpointInfo();
        endpoint.setProperty("MessageLogger", null);

        interceptor.handleMessage(message);
        assertThat(endpoint.getProperty("MessageLogger", Logger.class).getName(), is(CXFMaskSAMLTokenLoggingOutInterceptor.class.getName() + ".serviceLocal.endpointLocal.ifaceLocal"));
    }

    private Message createMessage() throws Exception {
        QName serviceName = new QName("serviceUri", "serviceLocal");
        Service service = new ServiceImpl();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(serviceName);

        EndpointInfo endpointInfo = new EndpointInfo();
        endpointInfo.setService(serviceInfo);
        QName endpointName = new QName("endpointUri", "endpointLocal");
        endpointInfo.setName(endpointName);

        Endpoint endpoint = new EndpointImpl(null, service, endpointInfo);

        Exchange exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, endpoint);

        QName ifaceName = new QName("ifaceUri", "ifaceLocal");
        InterfaceInfo iface = new InterfaceInfo(serviceInfo, ifaceName);
        serviceInfo.setInterface(iface);

        Message message = new MessageImpl();
        message.setExchange(exchange);
        return message;
    }
}