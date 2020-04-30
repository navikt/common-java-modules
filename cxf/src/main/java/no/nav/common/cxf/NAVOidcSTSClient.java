package no.nav.common.cxf;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.cxf.saml.ClaimsCallbackHandler;
import no.nav.common.utils.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStoreFactory;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.common.auth.subject.SubjectHandler.getSsoToken;

public class NAVOidcSTSClient extends STSClient {
    private static final Logger logger = LoggerFactory.getLogger(NAVOidcSTSClient.class);
    private static TokenStore tokenStore;
    private final StsType stsType;

    public NAVOidcSTSClient(Bus bus, StsType stsType) {
        super(bus);
        this.stsType = stsType;
        switch (stsType){
            case ON_BEHALF_OF_WITH_JWT:
                setOnBehalfOf(new OnBehalfOfWithOidcCallbackHandler());
                break;
            case EXTERNAL_SSO:
                setClaimsCallbackHandler(new ClaimsCallbackHandler());
                break;
        }
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
        if (stsType == StsType.SYSTEM_USER_IN_FSS) {
            return "systemSAML";
        } else {
            return getSsoToken()
                    .map(SsoToken::getToken)
                    .orElseThrow(() -> new IllegalStateException("Finner ingen sso token som kan bli cache-nøkkel for brukerens SAML-token"));
        }
    }

    private String getUserId() {
        return stsType == StsType.SYSTEM_USER_IN_FSS
                ? StringUtils.toString(getProperty(SecurityConstants.USERNAME))
                : SubjectHandler.getSubject().orElseThrow(() -> new RuntimeException("Klarte ikke å hente uid fra subject")).getUid();
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