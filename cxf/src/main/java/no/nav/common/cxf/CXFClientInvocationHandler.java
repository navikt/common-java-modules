package no.nav.common.cxf;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;

class CXFClientInvocationHandler<T> implements InvocationHandler {

    private final InvocationHandler invocationHandler;

    CXFClientInvocationHandler(CXFClient<T> cxfClient) {
        this.invocationHandler = handler(cxfClient);
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, objects);
        }

        try {
            return invocationHandler.invoke(o, method, objects);
        } catch (InvocationTargetException ite) {
            throw unwrapToExpectedExceptionForTheConsumer(ite);
        }
    }

    private Throwable unwrapToExpectedExceptionForTheConsumer(InvocationTargetException ite) {
        return ite.getCause();
    }

    private InvocationHandler handler(CXFClient<T> cxfClient) {
        switch (cxfClient.stsMode) {
            case NO_STS:
                return noSts(cxfClient);
            case SYSTEM_USER:
                return systemUserSts(cxfClient);
            case SUBJECT:
                return subjectSts(cxfClient);
            default:
                throw new IllegalStateException();
        }
    }

    private InvocationHandler noSts(CXFClient<T> cxfClient) {
        return invocationHandler(newPort(cxfClient).port);
    }

    private InvocationHandler systemUserSts(CXFClient<T> cxfClient) {
        PortAndClient<T> port = newPort(cxfClient);
        STSConfigurationUtil.configureStsForSystemUserInFSS(port.client, cxfClient.stsConfig);
        return invocationHandler(port.port);
    }

    private InvocationHandler subjectSts(CXFClient<T> cxfClient) {
        PortAndClient<T> oidcClient = newPort(cxfClient);
        OidcClientWrapper.configureStsForOnBehalfOfWithJWT(oidcClient.client, cxfClient.stsConfig);

        SubjectClients<T> subjectClients = new SubjectClients<>(oidcClient);

        return (proxyInstance, method, args) -> method.invoke(resolveSubjectPort(subjectClients), args);
    }

    private T resolveSubjectPort(SubjectClients<T> subjectClients) {
        return subjectClients.oidcClient.port;
    }

    private InvocationHandler invocationHandler(T port) {
        return (proxyInstance, method, args) -> {
            try {
                return method.invoke(port, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        };
    }

    private PortAndClient<T> newPort(CXFClient<T> cxfClient) {
        T port = cxfClient.factoryBean.create(cxfClient.serviceClass);
        ((BindingProvider) port).getBinding().setHandlerChain(cxfClient.handlerChain);
        Client client = ClientProxy.getClient(port);
        disableCNCheckIfConfigured(client);
        return new PortAndClient<>(port, client);
    }

    private static void disableCNCheckIfConfigured(Client client) {
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        TLSClientParameters tlsClientParameters = ofNullable(httpConduit.getTlsClientParameters()).orElseGet(TLSClientParameters::new);
        if (Boolean.valueOf(getProperty("disable.ssl.cn.check", "false"))) {
            tlsClientParameters.setDisableCNCheck(true);
        }
        httpConduit.setTlsClientParameters(tlsClientParameters);
    }

    private static class PortAndClient<T> {
        private final T port;
        private final Client client;

        private PortAndClient(T port, Client client) {
            this.port = port;
            this.client = client;
        }
    }

    private static class SubjectClients<T> {
        private final PortAndClient<T> oidcClient;

        private SubjectClients(PortAndClient<T> oidcClient) {
            this.oidcClient = oidcClient;
        }
    }

}
