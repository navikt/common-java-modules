package no.nav.brukerdialog.security.jaspic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Map;

public class OidcServerAuthConfig implements ServerAuthConfig {
    private static final Logger log = LoggerFactory.getLogger(OidcServerAuthConfig.class);

    private final String messageLayer;
    private final String appContext;
    private final CallbackHandler callbackHandler;
    private final Map<String, String> providerProperties;
    private final ServerAuthModule serverAuthModule;

    public OidcServerAuthConfig(String messageLayer, String appContext, CallbackHandler callbackHandler, Map<String, String> providerProperties, ServerAuthModule serverAuthModule) {
        log.debug("Instancieted");
        this.messageLayer = messageLayer;
        this.appContext = appContext;
        this.callbackHandler = callbackHandler;
        this.providerProperties = providerProperties;
        this.serverAuthModule = serverAuthModule;
    }

    @Override
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties) throws AuthException {
        log.debug("getAuthContext");
        return new OidcServerAuthContext(callbackHandler, serverAuthModule);
    }

    @Override
    public String getMessageLayer() {
        return messageLayer;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        return appContext;
    }

    @Override
    public void refresh() {
        //NOOP
    }

    @Override
    public boolean isProtected() {
        return false;
    }
}
