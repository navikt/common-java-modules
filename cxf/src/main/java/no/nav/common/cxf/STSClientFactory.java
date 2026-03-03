package no.nav.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;

import javax.xml.namespace.QName;
import java.util.HashMap;

class STSClientFactory {

    static NAVOidcSTSClient createBasicSTSClient(Bus bus, String location, String username, String password, StsType stsType) {
        NAVOidcSTSClient stsClient = new NAVOidcSTSClient(bus, stsType);
        stsClient.setWsdlLocation("wsdl/ws-trust-1.4-service.wsdl");
        stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceProvider"));
        stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceSOAP"));
        stsClient.setEnableAppliesTo(false);
        stsClient.setAllowRenewing(false);

        try {
            // Endpoint must be set on clients request context
            // as the wrapping requestcontext is not available
            // when creating the client from WSDL (ref cxf-users mailinglist)
            Client client = stsClient.getClient();
            client.getRequestContext().put(Message.ENDPOINT_ADDRESS, location);
            new LoggingFeature().initialize(client, bus);
        } catch (BusException | EndpointException e) {
            throw new RuntimeException("Failed to set endpoint adress of STSClient", e);
        }

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, username);
        properties.put(SecurityConstants.PASSWORD, password);
        stsClient.setProperties(properties);
        return stsClient;
    }

    static Policy resolvePolicy(Client client, String policyUri) {
        PolicyBuilder policyBuilder = client.getBus().getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver("", policyBuilder);
        return resolver.resolveReference(policyUri);
    }
}
