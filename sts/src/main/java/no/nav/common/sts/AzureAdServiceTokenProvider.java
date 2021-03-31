package no.nav.common.sts;

import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

@Slf4j
public class AzureAdServiceTokenProvider implements ServiceToServiceTokenProvider {

    private final String defaultCluster;

    private final String defaultNamespace;

    private final ScopedTokenProvider scopedTokenProvider;

    public AzureAdServiceTokenProvider(String defaultCluster, String defaultNamespace, ScopedTokenProvider scopedTokenProvider) {
        this.defaultCluster = defaultCluster;
        this.defaultNamespace = defaultNamespace;
        this.scopedTokenProvider = scopedTokenProvider;
    }

    @Override
    public String getServiceToken(String serviceName) {
        return getServiceToken(defaultCluster, defaultNamespace, serviceName);
    }

    @Override
    public String getServiceToken(String cluster, String namespace, String serviceName) {
        String serviceIdentifier = createServiceIdentifier(cluster, namespace, serviceName);
        String scope = createScope(serviceIdentifier);

        return scopedTokenProvider.getToken(scope);
    }

    private static String createScope(String serviceIdentifier) {
        return format("api://%s/.default", serviceIdentifier);
    }

    private static String createServiceIdentifier(String cluster, String namespace, String serviceName) {
        return format("%s.%s.%s", cluster, namespace, serviceName);
    }

}
