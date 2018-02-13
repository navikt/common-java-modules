package com.github.wrm.pact.domain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;

/**
 * holding informations about a pactFile
 *
 * @author pmucha
 */
public class PactFile {

    final File file;
    final String consumer;
    final String provider;
    final JsonObject pact;

    private PactFile(File file, String consumer, String provider, JsonObject pact) {
        this.file = file;
        this.consumer = consumer;
        this.provider = provider;
        this.pact = pact;
    }

    /**
     * reads the given file end extracts pact-details
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public static PactFile readPactFile(File file) throws FileNotFoundException {
        FileReader reader = new FileReader(file);
        JsonElement jelement = new JsonParser().parse(reader);

        String provider = jelement.getAsJsonObject().get("provider").getAsJsonObject().get("name").getAsString();
        String consumer = jelement.getAsJsonObject().get("consumer").getAsJsonObject().get("name").getAsString();
        return new PactFile(file, consumer, provider, jelement.getAsJsonObject());
    }

    /**
     * merges two pacts if the provider and the consumer is the same
     * deletes the old files and creates a new file with merged pacts
     *
     * @param first
     * @param second
     * @return
     */
    public static PactFile mergePactsAndDeleteRemains(final PactFile first, final PactFile second) {
        try {
            first.pact.getAsJsonArray("interactions").addAll(second.pact.getAsJsonArray("interactions"));

            javaslang.collection.List.of(first.getFile(), second.getFile()).forEach(PactFile::deleteFileIfExists);

            String relativePath = Paths.get(first.getFile().getAbsolutePath()).getParent() + "/" + first.getConsumer() + "_" + first.getProvider() + ".json";
            File merged = new File(relativePath);

            deleteFileIfExists(merged);
            merged.createNewFile();

            PrintWriter pw = new PrintWriter(merged);
            pw.write(first.pact.toString());
            pw.close();

            return new PactFile(merged, first.getConsumer(), first.getProvider(), first.pact);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void deleteFileIfExists(final File file) {
        if (file.exists()) file.delete();
    }

    public File getFile() {
        return file;
    }

    public String getConsumer() {
        return consumer;
    }

    public String getProvider() {
        return provider;
    }

    public JsonObject getPact() {
        return pact;
    }

}
