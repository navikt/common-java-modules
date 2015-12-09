package no.nav.metrics;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class SensuHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetricsClient.class);

    public static JSONObject createJSON(String application, String output) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", application);
        jsonObject.put("type", "metric");
        jsonObject.put("output", output);
        jsonObject.put("status", 0);
        jsonObject.put("handlers", new JSONArray("[events]"));

        return jsonObject;
    }

    public static void report(JSONObject jsonObject) {
        logger.debug("JSON object to be sent to socket:", jsonObject.toString());

        try (Socket socket = new Socket()) {
            connectToSocket(socket);
            writeToSocket(socket, jsonObject);
        } catch (UnknownHostException e) {
            logger.error("Unknown host", e);
        } catch (IOException e) {
            logger.error("Couldn't get I/O for socket", e);
        }
    }

    private static void connectToSocket(Socket socket) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 3030); //TODO: Port skal være i en system property
        socket.connect(inetSocketAddress, 500);
    }

    private static void writeToSocket(Socket socket, JSONObject jsonObject) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            writer.write(jsonObject.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Couldn't get I/O for writer", e);
        }
    }
}
