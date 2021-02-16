package no.nav.common.cxf;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;


@Slf4j
public class AuthContextRule implements MethodRule {

    private AuthContext authContext;

    public AuthContextRule() {
    }

    public AuthContextRule(AuthContext authContext) {
        this.authContext = authContext;
    }

    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
        log.info("subject is: {}", authContext);
    }

    @Override
    public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AuthContextHolderThreadLocal.instance().withContext(authContext, statement::evaluate);
            }
        };
    }
}
