package no.nav.common.cxf;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;

public class OidcClientWrapper {

    public static void configureStsForOnBehalfOfWithJWT(Client client, StsConfig stsConfig) {
        client.getRequestContext().put(SecurityConstants.STS_CLIENT,
                STSClientFactory.createBasicSTSClient(client.getBus(), stsConfig.url, stsConfig.username, stsConfig.password, StsType.ON_BEHALF_OF_WITH_JWT));
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, false);
        setEndpointPolicyReference(client);
    }

    private static void setEndpointPolicyReference(Client client) {
        Policy policy = STSClientFactory.resolvePolicy(client, "classpath:JwtSTSPolicy.xml");
        setClientEndpointPolicy(client, policy);
    }

    private static void setClientEndpointPolicy(Client client, Policy policy) {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();

        PolicyEngine policyEngine = client.getBus().getExtension(PolicyEngine.class);
        SoapMessage message = new SoapMessage(Soap12.getInstance());
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, message);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, message));
    }
}
