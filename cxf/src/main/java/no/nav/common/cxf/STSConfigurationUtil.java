package no.nav.common.cxf;

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

public class STSConfigurationUtil {

    public static void configureStsForSystemUserInFSS(Client client, StsConfig stsConfig) {
        configureSts(client, StsType.SYSTEM_USER_IN_FSS, stsConfig);
    }

    private static void configureSts(Client client, StsType stsType, StsConfig stsConfig) {
        String location = stsConfig.url;
        String username = stsConfig.username;
        String password = stsConfig.password;

        new WSAddressingFeature().initialize(client, client.getBus());

        STSClient stsClient = createBasicSTSClient(client.getBus(), location, username, password, stsType);
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, stsType.allowCachingInEndpoint());
        setEndpointPolicyReference(client, "classpath:stspolicy.xml");
    }

    private static STSClient createBasicSTSClient(Bus bus, String location, String username, String password, StsType stsType) {
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
