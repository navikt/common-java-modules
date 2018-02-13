package com.github.wrm.pact.repository;

import com.github.wrm.pact.domain.PactFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.maven.plugin.logging.Log;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class BrokerRepositoryProvider implements RepositoryProvider {

    private final String url;
    private final String consumerVersion;
    private final Log log;
    private final Optional<String> username;
    private final Optional<String> password;
    private final boolean insecure;

    public BrokerRepositoryProvider(String url,
                                    String consumerVersion,
                                    Log log,
                                    Optional<String> username,
                                    Optional<String> password,
                                    boolean insecure) {
        this.url = url;
        this.consumerVersion = consumerVersion;
        this.log = log;
        this.username = username;
        this.password = password;
        this.insecure = insecure;
    }

    @Override
    public void uploadPacts(final List<PactFile> pacts, final Optional<String> tagName) throws Exception {
        for (PactFile pact : pacts) {
            uploadPact(pact);
            if(tagName.isPresent()) {
                tagPactVersion(pact, tagName.get());
            }
        }
    }


    @Override
    public void downloadPacts(String providerId, String tagName, File targetDirectory) throws Exception {
        downloadPactsFromLinks(downloadPactLinks(providerId, tagName), targetDirectory);
    }

    public void downloadPactsFromLinks(List<String> links, File targetDirectory) throws IOException {
        targetDirectory.mkdirs();

        for (String link : links) {
            downloadPactFromLink(targetDirectory, link);
        }
    }

    public List<String> downloadPactLinks(String providerId, String tagName) throws IOException {
        String path = buildDownloadLinksPath(providerId, tagName);
        List<String> links = new ArrayList<>();

        log.info("Downloading pact links from " + path);

        try {
            HttpUriRequest request = createRequest(new HttpGet(path));
            CloseableHttpResponse response = createClient().execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                handleRequestError("Downloading pact links failed. Pact Broker answered with status: " + statusCode
                        + " and message: ", response);

                return links;
            }

            try (Scanner scanner = new Scanner(response.getEntity().getContent(), StandardCharsets.UTF_8.displayName())) {
                JsonElement jelement = new JsonParser().parse(scanner.useDelimiter("\\A").next());

                JsonArray asJsonArray = jelement.getAsJsonObject().get("_links").getAsJsonObject().get("pacts")
                        .getAsJsonArray();

                asJsonArray.forEach(element -> {
                    links.add(element.getAsJsonObject().get("href").getAsString());
                });
            }

            response.close();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e);
        }

        return links;
    }

    private void uploadPact(PactFile pact) throws IOException {
        String path = buildUploadPath(pact);

        log.info("Uploading pact to " + path);

        try {
            byte[] content = Files.readAllBytes(Paths.get(pact.getFile().getAbsolutePath()));
            HttpPut httpPut = new HttpPut(path);
            ByteArrayEntity entity = new ByteArrayEntity(content);
            httpPut.setEntity(entity);
            HttpUriRequest request = createRequest(httpPut);
            CloseableHttpResponse response = createClient().execute(request);
            if (response.getStatusLine().getStatusCode() > 201) {
                handleRequestError("Uploading failed. Pact Broker answered with: ", response);
            }
            response.close();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e);
        }
    }

    private void tagPactVersion(PactFile pact, String tagName) throws IOException {
        String path = buildTaggingPath(pact, tagName);

        log.info("Tagging pact version with path: " + path);

        try {
            HttpUriRequest request = createRequest(new HttpPut(path));
            CloseableHttpResponse response = createClient().execute(request);
            if (response.getStatusLine().getStatusCode() > 201) {
                handleRequestError("Tagging pact version failed. Pact Broker answered with", response);
            }
            response.close();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e);
        }
    }

    private void downloadPactFromLink(File targetDirectory, String link) throws IOException {
        try {
            HttpUriRequest request = createRequest(new HttpGet(link));
            CloseableHttpResponse response = createClient().execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                handleRequestError("Downloading pact failed. Pact Broker answered with status: " + statusCode
                        + " and message: ", response);
                return;
            }

            log.info("Downloading pact from " + link);

            try (Scanner scanner = new Scanner(response.getEntity().getContent(), StandardCharsets.UTF_8.displayName())) {
                String pact = scanner.useDelimiter("\\A").next();

                JsonElement jelement = new JsonParser().parse(pact);

                String provider = jelement.getAsJsonObject().get("provider").getAsJsonObject().get("name").getAsString();
                String consumer = jelement.getAsJsonObject().get("consumer").getAsJsonObject().get("name").getAsString();

                String pactFileName = targetDirectory.getAbsolutePath() + "/" + consumer + "-" + provider + ".json";

                try (PrintWriter printWriter = new PrintWriter(pactFileName)) {
                    printWriter.write(pact);
                    log.info("Writing pact file to " + pactFileName);
                }
            }

            response.close();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e);
        }
    }

    private void addBasicAuthTo(HttpURLConnection connection)
    {
        if (username.isPresent() && password.isPresent()) {
            String userpass = username.get() + ":" + password.get();
            String basicAuth = "Basic " + encodeBase64URLSafeString(userpass.getBytes());
            connection.setRequestProperty ("Authorization", basicAuth);
        }
    }

    private void addBasicAuthTo(HttpUriRequest connection)
    {
        if (username.isPresent() && password.isPresent()) {
            String userpass = username.get() + ":" + password.get();
            String basicAuth = "Basic " + encodeBase64URLSafeString(userpass.getBytes());
            connection.setHeader("Authorization", basicAuth);
        }
    }

    private String buildUploadPath(PactFile pact) {
        return url + "/pacts/provider/" + pact.getProvider() + "/consumer/" + pact.getConsumer() + "/version/"
                + consumerVersion;
    }

    /*http://pact-broker/pacticipants/Zoo%20App/versions/1.0.0/tags/prod*/

    private String buildTaggingPath(PactFile pact, String tagName) {
        return url + "/pacticipants/" + pact.getConsumer() + "/versions/" + consumerVersion + "/tags/" + tagName;
    }

    private String buildDownloadLinksPath(String providerId, String tagName) {
        String downloadUrl = url + "/pacts/provider/" + providerId + "/latest";
        if(tagName != null && !tagName.isEmpty()) {
            return downloadUrl + "/" + tagName;
        }
        else
            return downloadUrl;
    }

    private CloseableHttpClient createClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        if (this.insecure) {
            HttpClientBuilder b = HttpClientBuilder.create();
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
            b.setSSLContext(sslContext);
            HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory).build();
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            b.setConnectionManager(connMgr);
            return b.build();
        }
        return HttpClientBuilder.create().build();
    }

    private HttpUriRequest createRequest(HttpUriRequest request) {
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("charset", StandardCharsets.UTF_8.displayName());
        addBasicAuthTo(request);
        return request;
    }

    private void handleRequestError(String message, CloseableHttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity != null && entity.getContentLength() > 0) {
            try (Scanner scanner = new Scanner(entity.getContent(), StandardCharsets.UTF_8.displayName())) {
                log.error(message + scanner.useDelimiter("\\A").next());
            } catch (IOException e) {
                log.error(e);
            }
        }
    }
}
