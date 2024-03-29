package no.nav.common.cxf;

import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

import javax.xml.namespace.QName;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;

public class CXFClient<T> {

    public final JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
    final Class<T> serviceClass;
    final List<Handler> handlerChain = new ArrayList<>();

    STSMode stsMode = STSMode.NO_STS;
    StsConfig stsConfig;
    private int connectionTimeout = TimeoutFeature.DEFAULT_CONNECTION_TIMEOUT;
    private int receiveTimeout = TimeoutFeature.DEFAULT_RECEIVE_TIMEOUT;

    public CXFClient(Class<T> serviceClass) {
        boolean loggSecurityHeader = Boolean.getBoolean("no.nav.common.cxf.cxfclient.logging.logg-securityheader");
        factoryBean.getFeatures().add(new LoggingFeatureUtenBinaryOgUtenSamlTokenLogging(!loggSecurityHeader));
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

    public CXFClient<T> configureStsForSubject(StsConfig stsConfig) {
        this.stsMode = STSMode.SUBJECT;
        this.stsConfig = stsConfig;
        return this;
    }

    public CXFClient<T> configureStsForSystemUser(StsConfig stsConfig) {
        this.stsMode = STSMode.SYSTEM_USER;
        this.stsConfig = stsConfig;
        return this;
    }


    public CXFClient<T> withProperty(String key, Object value) {
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

    public CXFClient<T> serviceName(QName serviceName) {
        factoryBean.setServiceName(serviceName);
        return this;
    }

    public CXFClient<T> endpointName(QName endpointName) {
        factoryBean.setEndpointName(endpointName);
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
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{serviceClass},
                new CXFClientInvocationHandler(this)
        );
    }

}
