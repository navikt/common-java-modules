package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.Policy;

import javax.xml.namespace.QName;
import java.util.HashMap;

import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.STS_URL_KEY;

public class STSConfigurationUtil {

    public static void configureStsForSystemUserInFSS(Client client) {
        String location = requireProperty(STS_URL_KEY);
        String username = requireProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
        String password = requireProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

        new WSAddressingFeature().initialize(client, client.getBus());

        STSClient stsClient = createBasicSTSClient(client.getBus(), location, username, password);
        client.getRequestContext().put("ws-security.sts.client", stsClient);
        setEndpointPolicyReference(client, "classpath:stspolicy.xml");
    }

    private static STSClient createBasicSTSClient(Bus bus, String location, String username, String password) {
        NAVOidcSTSClient stsClient = new NAVOidcSTSClient(bus);
        stsClient.setWsdlLocation("wsdl/ws-trust-1.4-service.wsdl");
        stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceProvider"));
        stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceSOAP"));
        stsClient.setEnableAppliesTo(false);
        stsClient.setAllowRenewing(false);

        try {
            // Endpoint must be set on clients request context
            // as the wrapping requestcontext is not available
            // when creating the client from WSDL (ref cxf-users mailinglist)
            stsClient.getClient().getRequestContext().put(Message.ENDPOINT_ADDRESS, location);
        } catch (BusException | EndpointException e) {
            throw new RuntimeException("Failed to set endpoint adress of STSClient", e);
        }

        stsClient.getOutInterceptors().add(new LoggingOutInterceptor());
        stsClient.getInInterceptors().add(new LoggingInInterceptor());


        HashMap<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, username);
        properties.put(SecurityConstants.PASSWORD, password);
        stsClient.setProperties(properties);
        return stsClient;
    }

    private static void setEndpointPolicyReference(Client client, String uri) {
        Policy policy = resolvePolicyReference(client, uri);
        setClientEndpointPolicy(client, policy);
    }

    private static Policy resolvePolicyReference(Client client, String uri) {
        PolicyBuilder policyBuilder = client.getBus().getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver("", policyBuilder);
        return resolver.resolveReference(uri);
    }

    private static void setClientEndpointPolicy(Client client, Policy policy) {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();

        PolicyEngine policyEngine = client.getBus().getExtension(PolicyEngine.class);
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, client.getConduit(), null);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, null));
    }

    private static String requireProperty(String key) {
        String property = System.getProperty(key);
        if (property == null) {
            throw new RuntimeException("Required property " + key + " not available.");
        }
        return property;
    }
}
