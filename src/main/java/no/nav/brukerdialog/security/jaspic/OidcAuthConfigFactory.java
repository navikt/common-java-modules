package no.nav.brukerdialog.security.jaspic;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import java.util.Map;

public class OidcAuthConfigFactory extends AuthConfigFactory {
    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {
        return null;
    }

    @Override
    public String registerConfigProvider(String className, Map properties, String layer, String appContext, String description) {
        return null;
    }

    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext, String description) {
        return null;
    }

    @Override
    public boolean removeRegistration(String registrationID) {
        return false;
    }

    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        return new String[0];
    }

    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {
        return new String[0];
    }

    @Override
    public RegistrationContext getRegistrationContext(String registrationID) {
        return null;
    }

    @Override
    public void refresh() {

    }
}
