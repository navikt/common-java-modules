package no.nav.dialogarena.config.ssl;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.Test;

import static no.nav.dialogarena.config.ssl.SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER;
import static no.nav.dialogarena.config.ssl.SSLTestUtils.TRUST_ALL_SSL_CONTEXT_FACTORY;
import static org.assertj.core.api.Assertions.assertThat;

public class CxfCertificateValidationDisablerTest {

    @Test
    public void init(){
        CxfCertificateValidationDisabler.init();

        Aktoer_v2PortType aktoer_v2PortType = new CXFClient<>(Aktoer_v2PortType.class).build();

        Client client = ClientProxy.getClient(aktoer_v2PortType);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        assertThat(conduit.getTlsClientParameters().getSSLSocketFactory()).isSameAs(TRUST_ALL_SSL_CONTEXT_FACTORY);
        assertThat(conduit.getTlsClientParameters().getHostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

}