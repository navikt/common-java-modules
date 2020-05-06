package no.nav.common.cxf;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.Subject;
import no.nav.common.cxf.jetty.JettyTestServer;
import no.nav.common.test.junit.SystemPropertiesRule;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static no.nav.common.auth.subject.SsoToken.oidcToken;
import static no.nav.common.cxf.StsSecurityConstants.*;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;

@Slf4j
public class MetricsIntegrationTest extends JettyTestServer {

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule()
            .setProperty(NAIS_APP_NAME_PROPERTY_NAME, "cxf")
            .setProperty(STS_URL_KEY, "https://test-sts")
            .setProperty(SYSTEMUSER_USERNAME, "test-user")
            .setProperty(SYSTEMUSER_PASSWORD, "test-password");

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("test-subject", IdentType.EksternBruker, oidcToken("test-token", emptyMap())));

    @Test
    @Ignore // TODO: Ignore this until we add prometheus metrics
    public void client_generates_micrometer_metrics() {
        String url = startCxfServer(HelloWorld.class);

        HelloWorld noStsClient = new CXFClient<>(HelloWorld.class)
                .address(url)
                .build();


        HelloWorld systemUserClient = new CXFClient<>(HelloWorld.class)
                .configureStsForSystemUser()
                .address(url)
                .build();


        HelloWorld subjectClient = new CXFClient<>(HelloWorld.class)
                .configureStsForSubject()
                .address(url)
                .build();

        hi(noStsClient);
        hi(noStsClient);

        hi(systemUserClient);
        hi(systemUserClient);
        hi(systemUserClient);

        hi(subjectClient);
        hi(subjectClient);
        hi(subjectClient);
        hi(subjectClient);

//        List<MetricsTestUtils.PrometheusLine> scrape = MetricsTestUtils.scrape();
//
//        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 2)
//                .addLabel("sts", "NO_STS")
//                .addLabel("method", "sayHi")
//                .addLabel("success", "true")
//        ));
//
//        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 3)
//                .addLabel("sts", "SYSTEM_USER")
//                .addLabel("method", "sayHi")
//                .addLabel("success", "false")
//        ));
//
//        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 4)
//                .addLabel("sts", "SUBJECT")
//                .addLabel("method", "sayHi")
//                .addLabel("success", "false")
//        ));
    }

    private void hi(HelloWorld helloWorld) {
        try {
            helloWorld.sayHi("hi there!");
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
    }


}