package no.nav.sbl.dialogarena.common.cxf;

import no.nav.brukerdialog.security.context.SubjectHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NAVSTSClient extends STSClient {
    private static final Logger logger = LoggerFactory.getLogger(NAVSTSClient.class);
    public static final String DISABLE_CACHE_KEY = "NAVSTSClient.DISABLE_CACHE";
    private static TokenStore tokenStore;

    public NAVSTSClient(Bus b) {
        super(b);
    }

    @Override
    protected boolean useSecondaryParameters() {
        return false;
    }

    @Override
    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType, String binaryExchange) throws Exception {
        if (Boolean.getBoolean(DISABLE_CACHE_KEY)) {
            logger.debug("Cache is disabled, fetching from STS");
            return super.requestSecurityToken(appliesTo, action, requestType, binaryExchange);
        }

        ensureTokenStoreExists();

        String key = SubjectHandler.getSubjectHandler().getInternSsoToken();
        if (key == null) {
            throw new IllegalStateException("Kan ikke hente SAML uten OIDC");
        }

        SecurityToken token = tokenStore.getToken(key);
        if (token == null) {
            logger.debug("Missing token for {}, fetching it from STS", key);
            token = super.requestSecurityToken(appliesTo, action, requestType, binaryExchange);
            tokenStore.add(key, token);
        } else {
            logger.debug("Retrived token for {} from tokenStore", key);
        }
        return token;
    }

    private void ensureTokenStoreExists() {
        if (tokenStore == null) {
            createTokenStore();
        }
    }

    private synchronized void createTokenStore() {
        if (tokenStore == null) {
            logger.debug("Creating tokenStore");
            tokenStore = TokenStoreFactory.newInstance().newTokenStore(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message);
        }
    }
}
