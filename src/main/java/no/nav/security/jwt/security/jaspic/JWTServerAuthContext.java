package no.nav.security.jwt.security.jaspic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

public class JWTServerAuthContext implements ServerAuthContext {
    private static final Logger log = LoggerFactory.getLogger(JWTServerAuthContext.class);
    private final CallbackHandler callbackHandler;
    private final ServerAuthModule serverAuthModule;

    public JWTServerAuthContext(CallbackHandler callbackHandler, ServerAuthModule serverAuthModule) throws AuthException {
        this.callbackHandler = callbackHandler;
        this.serverAuthModule = serverAuthModule;

        serverAuthModule.initialize(null, null, callbackHandler, null);
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        return serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return serverAuthModule.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        serverAuthModule.cleanSubject(messageInfo, subject);
    }
}
