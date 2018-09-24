package no.nav.apiapp.selftest.impl;

import lombok.SneakyThrows;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.cxf.OidcClientWrapper;
import no.nav.sbl.dialogarena.common.cxf.STSConfigurationUtil;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyContainingAssertion;
import org.apache.neethi.PolicyOperator;
import org.apache.wss4j.policy.model.IssuedToken;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.stream.Stream;

import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.STS_URL_KEY;

public class STSHelsesjekk implements Helsesjekk {

    private final boolean sjekkOgsaOidcTilSAML;

    public STSHelsesjekk(boolean sjekkOgsaOidcTilSAML) {
        this.sjekkOgsaOidcTilSAML = sjekkOgsaOidcTilSAML;
    }

    @Override
    public void helsesjekk() throws Exception {
        new Sjekk().sjekk();
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "sts",
                System.getProperty(STS_URL_KEY),
                "Sjekker at systembruker kan hente token fra STS",
                true
        );
    }

    private class Sjekk {
        private Bus bus = BusFactory.getDefaultBus();
        private PolicyEngine policyEngine = bus.getExtension(PolicyEngine.class);
        private EndpointInfo endpointInfo = dummyEndpointInfo();
        private ClientImpl client = dummyClient();
        private MessageImpl message = dummyMessage();

        private void sjekk() throws Exception {
            stsClient(false).requestSecurityToken();

            if (sjekkOgsaOidcTilSAML) {
                SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();
                Subject systemUser = new Subject(systemUserTokenProvider.getUsername(), IdentType.Systemressurs, SsoToken.oidcToken(systemUserTokenProvider.getToken()));
                SubjectHandler.withSubject(systemUser, () -> {
                    stsClient(true).requestSecurityToken();
                });
            }
        }

        @SneakyThrows
        private ClientImpl dummyClient() {
            ServiceImpl service = new ServiceImpl();
            return new ClientImpl(bus, new EndpointImpl(bus, service, endpointInfo));
        }

        private EndpointInfo dummyEndpointInfo() {
            ServiceInfo serv = new ServiceInfo();
            serv.setInterface(new InterfaceInfo(serv, null));
            EndpointInfo endpointInfo = new EndpointInfo(serv, HTTPTransportFactory.DEFAULT_NAMESPACES.get(0));
            endpointInfo.setName(new QName("dummy"));
            return endpointInfo;
        }

        private MessageImpl dummyMessage() {
            MessageImpl message = new MessageImpl();
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.put(Bus.class, bus);
            message.setExchange(exchange);
            return message;
        }

        private STSClient stsClient(boolean useOidcToken) {
            if (useOidcToken) {
                OidcClientWrapper.configureStsForOnBehalfOfWithJWT(client);
            } else {
                STSConfigurationUtil.configureStsForSystemUserInFSS(client);
            }

            STSClient stsClient = (STSClient) client.getRequestContext().values().iterator().next();
            stsClient.setMessage(message);
            stsClient.setTemplate(getRequestSecurityTokenTemplate());
            return stsClient;
        }

        private Element getRequestSecurityTokenTemplate() {
            EndpointPolicy clientEndpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, client.getConduit(), null);
            return findAll(clientEndpointPolicy.getPolicy())
                    .filter(IssuedToken.class::isInstance)
                    .map(IssuedToken.class::cast)
                    .map(IssuedToken::getRequestSecurityTokenTemplate)
                    .findAny()
                    .orElseThrow(IllegalStateException::new);
        }
    }

    private Stream<PolicyComponent> findAll(PolicyComponent policyComponent) {
        return Stream.concat(Stream.of(policyComponent), findSubComponents(policyComponent));
    }

    private Stream<PolicyComponent> findSubComponents(PolicyComponent policyComponent) {
        if (policyComponent instanceof PolicyOperator) {
            return ((PolicyOperator) policyComponent).getPolicyComponents().stream().flatMap(this::findAll);
        } else if (policyComponent instanceof PolicyContainingAssertion) {
            return findAll(((PolicyContainingAssertion) policyComponent).getPolicy());
        } else {
            return Stream.empty();
        }
    }

}
