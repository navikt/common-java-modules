package no.nav.common.token_client.cache;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static no.nav.common.token_client.utils.TokenUtils.getSubject;

public enum AzureAdOnBehalfOfCacheKeyStrategy {

    SCOPE_AND_SUBJECT {
        @Override
        public String createCacheKey(String clientId, String tokenEndpointUrl, String tokenScope, String accessToken) {
            return tokenScope + "-" + getSubject(accessToken);
        }
    },

    ASSERTION_HASH {
        @Override
        public String createCacheKey(String clientId, String tokenEndpointUrl, String tokenScope, String accessToken) {
            getSubject(accessToken);
            requireValue(clientId, "Client ID");
            requireValue(tokenEndpointUrl, "Token endpoint URL");
            requireValue(tokenScope, "Token scope");

            MessageDigest digest = sha256();
            updateField(digest, clientId);
            updateField(digest, tokenEndpointUrl);
            updateField(digest, tokenScope);
            updateField(digest, HexFormat.of().formatHex(
                    sha256().digest(accessToken.getBytes(StandardCharsets.UTF_8))));
            return "azure-obo:v1:" + HexFormat.of().formatHex(digest.digest());
        }
    };

    public abstract String createCacheKey(
            String clientId, String tokenEndpointUrl, String tokenScope, String accessToken);

    private static void updateField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for token cache keys", e);
        }
    }

    private static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
