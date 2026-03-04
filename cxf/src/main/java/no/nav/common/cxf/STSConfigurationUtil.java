package no.nav.common.cxf;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;

public class STSConfigurationUtil {

    public static void configureStsForSystemUserInFSS(Client client, StsConfig stsConfig) {
        new WSAddressingFeature().initialize(client, client.getBus());

        client.getRequestContext().put(SecurityConstants.STS_CLIENT,
                STSClientFactory.createBasicSTSClient(client.getBus(), stsConfig.url, stsConfig.username, stsConfig.password, StsType.SYSTEM_USER_IN_FSS));
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, StsType.SYSTEM_USER_IN_FSS.allowCachingInEndpoint());
        setEndpointPolicyReference(client);
    }

    private static void setEndpointPolicyReference(Client client) {
        Policy policy = STSClientFactory.resolvePolicy(client, "classpath:stspolicy.xml");
        setClientEndpointPolicy(client, policy);
    }

    private static void setClientEndpointPolicy(Client client, Policy policy) {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();

        PolicyEngine policyEngine = client.getBus().getExtension(PolicyEngine.class);
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, client.getConduit(), null);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, null));
    }
}
