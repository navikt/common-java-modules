package no.nav.common.sts;

/**
 * Provides tokens for service to service authentication where each services requires a specific token
 */
public interface ServiceToServiceTokenProvider {

    String getServiceToken(String serviceName);

    String getServiceToken(String cluster, String namespace, String serviceName);

}
