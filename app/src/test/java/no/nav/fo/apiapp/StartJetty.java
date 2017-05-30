package no.nav.fo.apiapp;

import no.nav.apiapp.ApiAppServletContextListener;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

import static java.lang.System.setProperty;
import static no.nav.apiapp.ApiAppServletContextListener.SWAGGER_PATH;
import static no.nav.apiapp.rest.ExceptionMapper.MILJO_PROPERTY_NAME;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.SAML;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


public class StartJetty {

    public static void main(String[] args) {
        Jetty jetty = nyJetty(null, 8765);
        jetty.context.addFilter(RedirectToSwagger.class, "/*", EnumSet.allOf(DispatcherType.class));
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    public static Jetty nyJetty(String contextPath, int jettyPort) {
        setProperty(SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setProperty(MILJO_PROPERTY_NAME, "t");
        return Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml()
                .disableAnnotationScanning()
                .withLoginService(DevelopmentSecurity.jaasLoginModule(SAML))
                .buildJetty();
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

}
