package no.nav.brukerdialog.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.function.Function;

class TokenProviderUtil {

    public static <T> T getToken(Function<Client,Response> tokenRequestSupplier, Function<String, T> tokenExtractor) {
        return RestUtils.withClient(client -> {
            Response response = tokenRequestSupplier.apply(client);
            String responseString = response.readEntity(String.class);
            int status = response.getStatus();
            if (status == 200) {
                return tokenExtractor.apply(responseString);
            }
            throw new IllegalArgumentException("Could not refresh token with auth.server, got " + status + " and response " + responseString);
        });
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
