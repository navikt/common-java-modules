package no.nav.metrics.handlers;

import mockit.*;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import static no.nav.metrics.handlers.SensuHandler.createJSON;
import static org.junit.Assert.assertEquals;

public class SensuHandlerTest {

    @Test
    public void createJSONSetsTypeToMetric() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals("metric", jsonObject.get("type"));
    }

    @Test
    public void createJSONSetsStatusToZero() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals(0, jsonObject.get("status"));
    }

    @Test
    public void createJSONSetsHandlersToEvents() {
        JSONObject jsonObject = createJSON("test", "test");
        String handlers = jsonObject.get("handlers").toString();

        assertEquals("[\"events_nano\"]", handlers);
    }

    @Test
    public void skriverJsonTilSocket(@Mocked Socket socket, @Mocked final BufferedWriter writer) throws Exception {
        SensuHandler sensuHandler = new SensuHandler();
        sensuHandler.report("testApp", "testOutput");
        sensuHandler.report("testApp2", "testOutput2");

        Thread.sleep(100); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt

        new Verifications() {{
            writer.write("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
            writer.write("{\"status\":0,\"name\":\"testApp2\",\"output\":\"testOutput2\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        }};

    }

    @Test
    public void senderDataPaNyNarSocketConnectionFeiler(@Mocked final Socket socket, @Mocked final BufferedWriter writer) throws Exception {
        new Expectations() {{
            socket.connect((SocketAddress) any, anyInt);
            result = new IOException("dummy connection feil"); // Første kallet feiler
            result = null;
        }};

        SensuHandler sensuHandler = new SensuHandler();
        sensuHandler.report("testApp", "testOutput");

        Thread.sleep(100); // "Socketen" kjører i annen tråd, venter til vi kan anta den har gjort sitt

        new Verifications() {{
            writer.write("{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        }};

    }

}