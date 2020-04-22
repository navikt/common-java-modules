package no.nav.common.cxf;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.IdentType;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectRule;
import no.nav.common.metrics.prometheus.MetricsTestUtils;
import no.nav.common.test.junit.SystemPropertiesRule;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyMap;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.common.cxf.StsSecurityConstants.*;
import static no.nav.common.metrics.prometheus.MetricsTestUtils.equalCounter;
import static no.nav.common.utils.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static no.nav.common.utils.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MetricsIntegrationTest extends JettyTestServer {

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule()
            .setProperty(APP_NAME_PROPERTY_NAME, "cxf")
            .setProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, "t")
            .setProperty(STS_URL_KEY, "https://test-sts")
            .setProperty(SYSTEMUSER_USERNAME, "test-user")
            .setProperty(SYSTEMUSER_PASSWORD, "test-password");

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("test-subject", IdentType.EksternBruker, oidcToken("test-token", emptyMap())));

    @Test
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

        List<MetricsTestUtils.PrometheusLine> scrape = MetricsTestUtils.scrape();

        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 2)
                .addLabel("sts", "NO_STS")
                .addLabel("method", "sayHi")
                .addLabel("success", "true")
        ));

        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 3)
                .addLabel("sts", "SYSTEM_USER")
                .addLabel("method", "sayHi")
                .addLabel("success", "false")
        ));

        assertThat(scrape).anySatisfy(equalCounter(new MetricsTestUtils.PrometheusLine("cxf_client_seconds_count", 4)
                .addLabel("sts", "SUBJECT")
                .addLabel("method", "sayHi")
                .addLabel("success", "false")
        ));
    }

    private void hi(HelloWorld helloWorld) {
        try {
            helloWorld.sayHi("hi there!");
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
    }


}