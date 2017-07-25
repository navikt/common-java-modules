package no.nav.brukerdialog.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class TokenProviderUtil {

    public static <T> T getToken(Supplier<HttpUriRequest> tokenRequestSupplier, Function<String, T> tokenExtractor) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpUriRequest request = tokenRequestSupplier.get();
            try (CloseableHttpResponse response = client.execute(request)) {
                String responseString = responseText(response);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return tokenExtractor.apply(responseString);
                }
                throw new IllegalArgumentException("Could not refresh token with auth.server, got " + response.getStatusLine().getStatusCode() + " and response " + responseString);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String responseText(CloseableHttpResponse response) throws IOException {
        return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    static String findToken(String responseString, String tokenName) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(responseString);
        JsonNode token = json.get(tokenName);
        if (token == null) {
            throw new OidcTokenException("mangler attributt i respons: " + tokenName);
        }
        return token.textValue();
    }

    static String basicCredentials(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes());
    }

}
