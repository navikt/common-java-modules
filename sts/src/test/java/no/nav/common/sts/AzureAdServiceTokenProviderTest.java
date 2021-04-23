package no.nav.common.sts;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class AzureAdServiceTokenProviderTest {

    @Test
    public void skal_lage_riktig_scope() {
        ScopedTokenProvider scopedTokenProvider = mock(ScopedTokenProvider.class);

        AzureAdServiceTokenProvider serviceTokenProvider =
                new AzureAdServiceTokenProvider(scopedTokenProvider);

        serviceTokenProvider.getServiceToken("my-app", "test-namespace", "test-cluster");

        verify(scopedTokenProvider, times(1)).getToken("api://test-cluster.test-namespace.my-app/.default");
    }

}
