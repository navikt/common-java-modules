package no.nav.metrics.handlers;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SensuHandlerTest {

    @Test
    public void skriverJsonTilSocket(@Mocked Socket socket, @Mocked final BufferedWriter writer) throws Exception {
        SensuHandler sensuHandler = new SensuHandler("testApp");
        sensuHandler.report("testOutput");

        Thread.sleep(100); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt

        final JSONObject forventet = new JSONObject("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");

        new Verifications() {{
            List<String> writtenJSON = new ArrayList<>();

            writer.write(withCapture(writtenJSON));
            String json = finnJsonString(writtenJSON);

            assertTrue(new JSONObject(json).similar(forventet));
        }};

    }

    @Test
    public void senderDataPaNyNarSocketConnectionFeiler(@Mocked final Socket socket, @Mocked final BufferedWriter writer) throws Exception {
        new Expectations() {{
            socket.connect((SocketAddress) any, anyInt);
            result = new IOException("dummy connection feil"); // Første kallet feiler
            result = null;
        }};

        SensuHandler sensuHandler = new SensuHandler("testApp");
        sensuHandler.report("testOutput");

        Thread.sleep(1100); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt (1000ms delay pga feilende kall + litt)

        final JSONObject forventet = new JSONObject("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        new Verifications() {{
            String writtenJSON;

            writer.write(writtenJSON = withCapture());
            assertTrue(new JSONObject(writtenJSON).similar(forventet));
        }};


    }

    private String finnJsonString(List<String> writtenStrings) {
        return writtenStrings.stream()
                .filter(s -> s.startsWith("{"))
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException(writtenStrings.toString()));
    }

}