package no.nav.dialogarena.config.ssl;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.common.cxf.NAVOidcSTSClient;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.SecurityConstants;
import org.junit.Test;

import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.TRUST_ALL_SSL_SOCKET_FACTORY;
import static org.assertj.core.api.Assertions.assertThat;

public class CxfCertificateValidationDisablerTest {

    @Test
    public void init() throws Exception {
        setProperty(STS_URL_KEY, "");
        setProperty(SYSTEMUSER_USERNAME, "");
        setProperty(SYSTEMUSER_PASSWORD, "");

        SSLTestUtils.disableCertificateChecks();

        Aktoer_v2PortType aktoer_v2PortType = new CXFClient<>(Aktoer_v2PortType.class)
                .configureStsForSystemUser()
                .build();

        Client client = ClientProxy.getClient(aktoer_v2PortType);
        sjekkAtSertifikatSjekkerErDisablet(client);

        NAVOidcSTSClient navstsClient = (NAVOidcSTSClient) client.getRequestContext().get(SecurityConstants.STS_CLIENT);
        sjekkAtSertifikatSjekkerErDisablet(navstsClient.getClient());
    }

    private void sjekkAtSertifikatSjekkerErDisablet(Client client) {
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        TLSClientParameters tlsClientParameters = conduit.getTlsClientParameters();
        assertThat(tlsClientParameters).isNotNull();
        assertThat(tlsClientParameters.isDisableCNCheck()).isTrue();
        assertThat(tlsClientParameters.getSSLSocketFactory()).isSameAs(TRUST_ALL_SSL_SOCKET_FACTORY);
        assertThat(tlsClientParameters.getHostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

}