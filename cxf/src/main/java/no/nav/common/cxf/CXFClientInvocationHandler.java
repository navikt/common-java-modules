package no.nav.common.cxf;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.xml.ws.BindingProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;

class CXFClientInvocationHandler<T> implements InvocationHandler {

    private final InvocationHandler invocationHandler;
    private final STSMode stsMode;

    CXFClientInvocationHandler(CXFClient<T> cxfClient) {
        this.stsMode = cxfClient.stsMode;
        this.invocationHandler = handler(cxfClient);
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, objects);
        }

        boolean success = false;
        long startTime = System.currentTimeMillis();
        try {
            Object result = invocationHandler.invoke(o, method, objects);
            success = true;
            return result;
        } catch (InvocationTargetException ite) {
            throw unwrapToExpectedExceptionForTheConsumer(ite);
        } finally {
            // TODO: Hvis vi trenger prometheus metrikker for dette, så send inn PrometheusMeterRegistry
            //  Hvis man ikke trenger metrikker, så kan man også vurdere å logge det til kibana

            //            meterRegistry.timer("cxf_client",
            //                    "method",
            //                    method.getName(),
            //                    "success",
            //                    Boolean.toString(success),
            //                    "sts",
            //                    stsMode.name()
            //            ).record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
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
        STSConfigurationUtil.configureStsForSystemUserInFSS(port.client);
        return invocationHandler(port.port);
    }

    private InvocationHandler subjectSts(CXFClient<T> cxfClient) {
        PortAndClient<T> openAmClient = newPort(cxfClient);
        STSConfigurationUtil.configureStsForExternalSSO(openAmClient.client);

        PortAndClient<T> oidcClient = newPort(cxfClient);
        OidcClientWrapper.configureStsForOnBehalfOfWithJWT(oidcClient.client);

        SubjectClients<T> subjectClients = new SubjectClients<>(
                openAmClient,
                oidcClient
        );
        return (proxyInstance, method, args) -> method.invoke(resolveSubjectPort(subjectClients), args);
    }

    private T resolveSubjectPort(SubjectClients<T> subjectClients) {
        Subject subject = SubjectHandler.getSubject().orElseThrow(() -> new IllegalStateException("no subject available"));
        SsoToken.Type tokenType = subject.getSsoToken().getType();
        switch (tokenType) {
            case OIDC:
                return subjectClients.oidcClient.port;
            case EKSTERN_OPENAM:
                return subjectClients.openAmClient.port;
            default:
                throw new IllegalStateException("illegal port type");
        }
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
        private final PortAndClient<T> openAmClient;
        private final PortAndClient<T> oidcClient;

        private SubjectClients(PortAndClient<T> openAmClient, PortAndClient<T> oidcClient) {
            this.openAmClient = openAmClient;
            this.oidcClient = oidcClient;
        }
    }

}
