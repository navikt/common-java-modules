package no.nav.common.cxf;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AuthContextRule implements BeforeEachCallback, AfterEachCallback {

    private AuthContext authContext;
    private AuthContext previousContext;

    public AuthContextRule() {}

    public AuthContextRule(AuthContext authContext) {
        this.authContext = authContext;
    }

    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        previousContext = AuthContextHolderThreadLocal.instance().getContext().orElse(null);
        AuthContextHolderThreadLocal.instance().setContext(authContext);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        AuthContextHolderThreadLocal.instance().setContext(previousContext);
    }
}
