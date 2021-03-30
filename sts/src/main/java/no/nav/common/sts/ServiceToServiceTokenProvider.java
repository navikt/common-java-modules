package no.nav.common.sts;

public interface ServiceToServiceTokenProvider {

    String getServiceToken(String serviceName);

    String getServiceToken(String cluster, String namespace, String serviceName);

}
