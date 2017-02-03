package no.nav.sbl.dialogarena.common.abac;

import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


public class XacmlMapperTest {

    @Test
    public void convertsRequestToJson() throws IOException {

        final StringEntity stringEntity = XacmlMapper.mapRequestToEntity(MockXacmlRequest.getXacmlRequest());

        assertThat(stringEntity.getContentLength(), greaterThan(0L));

        String expectedContent = getExpectedContentRequest();
        assertThat(Utils.entityToString(stringEntity), is(expectedContent));
    }

    private String getExpectedContentRequest() throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource("xacmlrequest-withtoken.json");
        String path = getPathWithoutInitialSlashOnWindows(url);
        return Files.lines(Paths.get(path)).collect(Collectors.joining());
    }

    private String getPathWithoutInitialSlashOnWindows(URL url) {
        return url.getPath().replaceFirst("^/(.:/)", "$1");
    }
}