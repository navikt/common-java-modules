package no.nav.brukerdialog.security.jaspic;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.*;
import javax.security.auth.message.module.ServerAuthModule;
import java.util.Map;

public class OidcAuthConfigProvider implements AuthConfigProvider {

    private static final String CALLBACK_HANDLER_PROPERTY_NAME = "authconfigprovider.client.callbackhandler";

    private Map<String, String> providerProperties;
    private ServerAuthModule serverAuthModule;

    public OidcAuthConfigProvider(Map<String, String> properties, AuthConfigFactory factory) {
        this.providerProperties = properties;

        if (factory != null) {
            factory.registerConfigProvider(this, null, null, "Auto registration");
        }
    }

    public OidcAuthConfigProvider(ServerAuthModule serverAuthModule) {
        this.serverAuthModule = serverAuthModule;
    }

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return null;
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return new OidcServerAuthConfig(layer, appContext, handler == null ? createDefaultCallbackHandler() : handler, providerProperties, serverAuthModule);
    }

    @Override
    public void refresh() {
    }

    private CallbackHandler createDefaultCallbackHandler() throws AuthException {
        String callBackClassName = System.getProperty(CALLBACK_HANDLER_PROPERTY_NAME);

        if (callBackClassName == null) {
            throw new AuthException("No default handler set via system property: " + CALLBACK_HANDLER_PROPERTY_NAME);
        }

        try {
            return (CallbackHandler) Thread.currentThread().getContextClassLoader().loadClass(callBackClassName).newInstance();
        } catch (Exception e) {
            throw new AuthException(e.getMessage());
        }
    }
}
