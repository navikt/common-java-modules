package no.nav.common.oidc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

public class TokenRefresher {

    private static final CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static Optional<String> refreshIdToken(String refreshUrl, String refreshToken) {
        try (CloseableHttpResponse response = httpClient.execute(createRefreshPost(refreshUrl, refreshToken))) {
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String jsonData = EntityUtils.toString(entity);
                RefreshIdTokenResult result = jsonMapper.readValue(jsonData, RefreshIdTokenResult.class);
                return Optional.of(result.idToken);
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static HttpPost createRefreshPost(String refreshUrl, String refreshToken) throws JsonProcessingException, UnsupportedEncodingException {
        RefreshIdTokenDTO refreshIdTokenDTO = new RefreshIdTokenDTO();
        refreshIdTokenDTO.refreshToken = refreshToken;

        String postBody = jsonMapper.writeValueAsString(refreshIdTokenDTO);

        HttpPost post = new HttpPost(refreshUrl);
        post.setEntity(new StringEntity(postBody));

        return post;
    }

    public static class RefreshIdTokenDTO {
        public String refreshToken;
    }

    public static class RefreshIdTokenResult {
        public String idToken;
    }

}
