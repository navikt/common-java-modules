package no.nav.fo.apiapp;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import static java.lang.System.setProperty;
import static no.nav.apiapp.ApiAppServletContextListener.SWAGGER_PATH;
import static no.nav.apiapp.Constants.MILJO_PROPERTY_NAME;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.SAML;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


public class StartJetty {

    private static final String KJENT_APP = "veilarbaktivitet";
    private static final String KJENT_MILJO = "t6";

    public static void main(String[] args) {
        setupLogging();
        Jetty jetty = nyJetty(null, 8765);
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    public static Jetty nyJetty(String contextPath, int jettyPort) {
        return DevelopmentSecurity.setupISSO(
                defaultJetty(contextPath, jettyPort)
                , new DevelopmentSecurity.ISSOSecurityConfig(KJENT_APP, KJENT_MILJO)
        ).buildJetty();
    }

    public static Jetty nyJettyForTest(String contextPath, int jettyPort) {
        return DevelopmentSecurity.setupSamlLogin(
                defaultJetty(contextPath, jettyPort)
                , new DevelopmentSecurity.SamlSecurityConfig(KJENT_APP, KJENT_MILJO)
        ).buildJetty();
    }

    private static Jetty.JettyBuilder defaultJetty(String contextPath, int jettyPort) {
        Jetty.JettyBuilder jettyBuilder = Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml();
        jettyBuilder.buildJetty().context.addFilter(RedirectToSwagger.class, "/*", EnumSet.allOf(DispatcherType.class));
        return jettyBuilder;
    }

    public static class RedirectToSwagger implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
            if ("/".equals(((HttpServletRequest) req).getServletPath())) {
                ((HttpServletResponse) res).sendRedirect(req.getServletContext().getContextPath() + SWAGGER_PATH);
            } else {
                chain.doFilter(req, res);
            }
        }

        @Override
        public void destroy() {}
    }


    public static void setupLogging() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", "api-app");
    }

}
