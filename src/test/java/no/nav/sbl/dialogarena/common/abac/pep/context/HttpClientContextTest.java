package no.nav.sbl.dialogarena.common.abac.pep.context;

import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class HttpClientContextTest {
    private static final String KEY_READ_TIMEOUT = "abac.bibliotek.readTimeout";
    private static final String KEY_CONNECTION_TIMEOUT = "abac.bibliotek.connectionTimeout";
    private static final String SYSTEMUSER_PASSWORD = "systemuserpassword";
    private static final String SYSTEMUSER = "systemuser";

    @BeforeClass
    public static void setup() {
        setProperty("no.nav.abac.systemuser.username", SYSTEMUSER);
        setProperty("no.nav.abac.systemuser.password", SYSTEMUSER_PASSWORD);
    }

    @After
    public void cleanUp() {
        System.clearProperty(KEY_CONNECTION_TIMEOUT);
        System.clearProperty(KEY_READ_TIMEOUT);
    }

    @Test
    public void getsDefaultTimeoutWhenNoProperty() throws IOException, NoSuchFieldException, AbacException {
        final int expectedConnectionTimeout = 500;
        final int expectedReadTimeout = 1500;

        final RequestConfig requestConfig = HttpClientContext.createConfigForTimeout();

        assertThat(requestConfig.getConnectionRequestTimeout(), is(expectedConnectionTimeout));
        assertThat(requestConfig.getSocketTimeout(), is((expectedReadTimeout)));
        assertThat(requestConfig.getConnectTimeout(), is(expectedConnectionTimeout));
    }

    @Test
    public void getsTimeoutFromFromPropertyWhenPresent() throws IOException, NoSuchFieldException, AbacException {
        final String expectedConnectionTimeout = "1000";
        final String expectedReadTimeout = "2000";

        setProperty(KEY_CONNECTION_TIMEOUT, expectedConnectionTimeout);
        setProperty(KEY_READ_TIMEOUT, expectedReadTimeout);

        final RequestConfig requestConfig = HttpClientContext.createConfigForTimeout();

        assertThat(requestConfig.getConnectionRequestTimeout(), is(Integer.parseInt(expectedConnectionTimeout)));
        assertThat(requestConfig.getSocketTimeout(), is(Integer.parseInt(expectedReadTimeout)));
        assertThat(requestConfig.getConnectTimeout(), is(Integer.parseInt(expectedConnectionTimeout)));
    }

    @Test
    public void addsCredentialsToRequest() throws NoSuchFieldException, IOException, AbacException {
        final CredentialsProvider credentialsProvider = HttpClientContext.addSystemUserToRequest();

        assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getPassword(), is(SYSTEMUSER_PASSWORD));
        assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getUserPrincipal().getName(), is(SYSTEMUSER));
    }
}