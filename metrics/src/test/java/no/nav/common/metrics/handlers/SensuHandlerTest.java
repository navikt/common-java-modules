package no.nav.common.metrics.handlers;

import no.nav.common.metrics.TestUtil;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

public class SensuHandlerTest {

    private ServerSocket serverSocket;

    @Before
    public void setup() throws IOException {
        serverSocket = new ServerSocket(0);
    }

    @Test
    public void skriverJsonTilSocket() throws Exception {
        AtomicReference<JSONObject> jsonObjectAtomicReference = new AtomicReference<>();
        SensuHandler sensuHandler = new SensuHandler(TestUtil.testConfig(serverSocket.getLocalPort())) {
            @Override
            void writeToSensu(JSONObject jsonObject, Socket socket) throws IOException {
                jsonObjectAtomicReference.set(jsonObject);
                super.writeToSensu(jsonObject, socket);
            }
        };
        sensuHandler.report("testOutput");
        Thread.sleep(500); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt

        final JSONObject forventet = new JSONObject("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        assertTrue(jsonObjectAtomicReference.get().similar(forventet));


    }


    @Test
    public void senderDataPaNyNarSocketConnectionFeiler() throws Exception {
        AtomicReference<JSONObject> jsonObjectAtomicReference = new AtomicReference<>();
        SensuHandler sensuHandler = new SensuHandler(TestUtil.testConfig(serverSocket.getLocalPort())) {
            int attempt = 0;

            @Override
            void writeToSensu(JSONObject jsonObject, Socket socket) throws IOException {
                if (attempt == 0) {
                    attempt += 1;
                    throw new IOException("dummy connection feil");
                }
                jsonObjectAtomicReference.set(jsonObject);
                super.writeToSensu(jsonObject, socket);
            }
        };

        sensuHandler.report("testOutput");
        Thread.sleep(1100); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt (1000ms delay pga feilende kall + litt)

        final JSONObject forventet = new JSONObject("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        assertTrue(jsonObjectAtomicReference.get().similar(forventet));


    }

    private String finnJsonString(List<String> writtenStrings) {
        return writtenStrings.stream()
                .filter(s -> s.startsWith("{"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(writtenStrings.toString()));
    }
}
