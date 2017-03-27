package no.nav.sbl.dialogarena.common.abac.pep.service;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.SocketException;

import static java.lang.System.setProperty;

@RunWith(MockitoJUnitRunner.class)

public class AbacTest {

    @InjectMocks
    Abac abac;

    @Test(expected = SocketException.class)
    public void canSimulateAvbrudd() throws IOException, NoSuchFieldException {
        setProperty("abac.bibliotek.simuler.avbrudd", "true");

        abac.isAuthorized(RequestConfig.custom().build(), new HttpPost(), new BasicCredentialsProvider());
    }

}