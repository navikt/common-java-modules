package no.nav.sbl.dialogarena.common.cxf;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.util.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStoreFactory;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.sbl.dialogarena.common.cxf.StsType.SYSTEM_USER_IN_FSS;

public class NAVOidcSTSClient extends STSClient {
    private static final Logger logger = LoggerFactory.getLogger(NAVOidcSTSClient.class);
    private static TokenStore tokenStore;
    private final StsType stsType;

    public NAVOidcSTSClient(Bus bus, StsType stsType) {
        super(bus);
        this.stsType = stsType;
    }

    @Override
    protected boolean useSecondaryParameters() {
        return false;
    }

    @Override
    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType, String binaryExchange) throws Exception {
        ensureTokenStoreExists();

        String userId = getUserId();
        String key = getTokenStoreKey();

        SecurityToken token = tokenStore.getToken(key);
        if (token == null) {
            logger.debug("Missing token for user {}, fetching it from STS", userId);
            token = super.requestSecurityToken(appliesTo, action, requestType, binaryExchange);
            tokenStore.add(key, token);
        } else {
            logger.debug("Retrived token for user {} from tokenStore", userId);
        }
        return token;
    }

    private String getTokenStoreKey() {
        return stsType.name() + "-" + getUserKey();
    }

    private String getUserKey() {
        if (stsType == SYSTEM_USER_IN_FSS) {
            return "systemSAML";
        }

        String internSsoToken = SubjectHandler.getSubjectHandler().getInternSsoToken();
        if (internSsoToken == null) {
            logger.info("Finner ingen OIDC, henter SAML som systembruker");
            return "systemSAML";
        } else {
            return internSsoToken;
        }
    }

    private String getUserId() {
        return stsType == SYSTEM_USER_IN_FSS ? StringUtils.toString(getProperty(SecurityConstants.USERNAME)) : SubjectHandler.getSubjectHandler().getUid();
    }

    private void ensureTokenStoreExists() {
        if (tokenStore == null) {
            createTokenStore();
        }
    }

    private synchronized void createTokenStore() {
        if (tokenStore == null) {
            logger.debug("Creating tokenStore");
            tokenStore = new MemoryTokenStoreFactory().newTokenStore(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message);
        }
    }
}