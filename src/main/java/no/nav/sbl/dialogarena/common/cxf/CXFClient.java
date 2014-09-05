package no.nav.sbl.dialogarena.common.cxf;

import no.nav.modig.security.sts.utility.STSConfigurationUtility;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;

public class CXFClient<T> {

    public final JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
    private final Class<T> serviceClass;
    private final List<Handler> handlerChain = new ArrayList<>();
    private boolean configureStsForExternalSSO, configureStsForSystemUser;
    private int connectionTimeout = TimeoutFeature.DEFAULT_CONNECTION_TIMEOUT;
    private int receiveTimeout = TimeoutFeature.DEFAULT_RECEIVE_TIMEOUT;

    public CXFClient(Class<T> serviceClass) {
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.setProperties(new HashMap<String, Object>());
        this.serviceClass = serviceClass;
    }

    public CXFClient<T> address(String url) {
        factoryBean.setAddress(url);
        return this;
    }

    public CXFClient<T> wsdl(String url) {
        factoryBean.setWsdlURL(url);
        return this;
    }

    public CXFClient<T> configureStsForExternalSSO() {
        configureStsForExternalSSO = true;
        return this;
    }

    public CXFClient<T> configureStsForSystemUser() {
        configureStsForSystemUser = true;
        return this;
    }

    public CXFClient<T> setProperty(String key, Object value) {
        factoryBean.getProperties().put(key, value);
        return this;
    }

    public CXFClient<T> timeout(int connectionTimeout, int receiveTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
        return this;
    }

    public CXFClient<T> enableMtom() {
        factoryBean.getProperties().put("mtom-enabled", true);
        return this;
    }

    public CXFClient<T> withHandler(Handler handler, Handler... moreHandlers) {
        handlerChain.add(handler);
        handlerChain.addAll(asList(moreHandlers));
        return this;
    }

    @SafeVarargs
    public final CXFClient<T> withOutInterceptor(Interceptor<? extends Message> interceptor, Interceptor<? extends Message>... moreInterceptors) {
        List<Interceptor<? extends Message>> outInterceptors = factoryBean.getOutInterceptors();
        outInterceptors.add(interceptor);
        outInterceptors.addAll(asList(moreInterceptors));
        return this;
    }

    public T build() {
        factoryBean.getFeatures().add(new TimeoutFeature(receiveTimeout, connectionTimeout));
        T portType = factoryBean.create(serviceClass);
        Client client = ClientProxy.getClient(portType);
        disableCNCheckIfConfigured(client);
        if (configureStsForExternalSSO) {
            STSConfigurationUtility.configureStsForExternalSSO(client);
        }
        if (configureStsForSystemUser) {
            STSConfigurationUtility.configureStsForSystemUser(client);
        }
        ((BindingProvider) portType).getBinding().setHandlerChain(handlerChain);
        return portType;
    }

    private static void disableCNCheckIfConfigured(Client client) {
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        httpConduit.setTlsClientParameters(new TLSClientParameters());
        if (Boolean.valueOf(System.getProperty("disable.ssl.cn.check", "false"))) {
            httpConduit.getTlsClientParameters().setDisableCNCheck(true);
        }
    }

}
