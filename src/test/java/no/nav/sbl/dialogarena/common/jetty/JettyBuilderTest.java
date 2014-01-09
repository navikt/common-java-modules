package no.nav.sbl.dialogarena.common.jetty;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.junit.Test;
import sun.jdbc.odbc.ee.ConnectionPoolDataSource;

import java.io.File;

public class JettyBuilderTest {

    @Test
    public void startJetty() {
        Jetty jetty = new Jetty.JettyBuilder()
                .war(new File("/path/to/file"))
                .at("contextpath")
                .port(8888)
                .overrideWebXml(new File("path/to/webxml"))
                .withLoginService(new JAASLoginService())
                .addDatasource(new ConnectionPoolDataSource(), "jndi-name")
                .buildJetty();

        jetty.start();
        jetty.stop.run();
    }
}
