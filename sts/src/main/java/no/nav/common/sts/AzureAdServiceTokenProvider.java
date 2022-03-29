package no.nav.common.sts;

import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

/**
 * Deprecated: Succeeded by token-client module
 */
@Deprecated
@Slf4j
public class AzureAdServiceTokenProvider implements ServiceToServiceTokenProvider {

    private final ScopedTokenProvider scopedTokenProvider;

    public AzureAdServiceTokenProvider(ScopedTokenProvider scopedTokenProvider) {
        this.scopedTokenProvider = scopedTokenProvider;
    }

    @Override
    public String getServiceToken(String serviceName, String namespace, String cluster) {
        String serviceIdentifier = createServiceIdentifier(serviceName, namespace, cluster);
        String scope = createScope(serviceIdentifier);

        return scopedTokenProvider.getToken(scope);
    }

    private static String createScope(String serviceIdentifier) {
        return format("api://%s/.default", serviceIdentifier);
    }

    private static String createServiceIdentifier(String serviceName, String namespace, String cluster) {
        return format("%s.%s.%s", cluster, namespace, serviceName);
    }

}
