package no.nav.fo.apiapp;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import java.io.IOException;
import java.net.ServerSocket;

import static java.lang.System.setProperty;
import static no.nav.apiapp.rest.ExceptionMapper.MILJO_PROPERTY_NAME;
import static no.nav.dialogarena.config.DevelopmentSecurity.LoginModuleType.SAML;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


public class StartJetty {

    public static void main(String[] args) {
        Jetty jetty = nyJetty(null);
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    public static Jetty nyJetty(String contextPath) {
        setProperty(SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setProperty(MILJO_PROPERTY_NAME, "t");
        return Jetty.usingWar()
                .at(contextPath)
                .port(tilfeldigPort())
                .overrideWebXml()
                .disableAnnotationScanning()
                .withLoginService(DevelopmentSecurity.jaasLoginModule(SAML))
                .buildJetty();
    }

    private static int tilfeldigPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
