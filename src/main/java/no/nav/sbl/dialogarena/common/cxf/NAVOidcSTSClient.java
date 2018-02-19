package no.nav.sbl.dialogarena.common.cxf;

import no.nav.brukerdialog.security.context.SubjectHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class NAVOidcSTSClient extends STSClient {
    private static final Logger logger = LoggerFactory.getLogger(NAVOidcSTSClient.class);
    private static volatile TokenStore tokenStore;
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
        String internSsoToken = SubjectHandler.getSubjectHandler().getInternSsoToken();
        if (internSsoToken == null) {
            logger.info("Finner ingen OIDC, henter SAML som systembruker");
            return "systemSAML";
        } else {
            return internSsoToken;
        }
    }

    private String getUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private void ensureTokenStoreExists() {
        synchronized (NAVOidcSTSClient.class) {
            Optional<EndpointInfo> info = ofNullable(message)
                    .map(Message::getExchange)
                    .map(Exchange::getEndpoint)
                    .map(Endpoint::getEndpointInfo);
            if (tokenStore == null) {
                TokenStore ts = (TokenStore) message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                if (ts == null) {
                    ts = (TokenStore) info.map(i -> i.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE)).orElseGet(MemoryTokenStore::new);
                }
                tokenStore = ts;
            }
            message.put(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            info.ifPresent(i -> i.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore));
        }
    }
}