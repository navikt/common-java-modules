package no.nav.sbl.dialogarena.common.abac;

import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static java.nio.file.Paths.get;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


public class XacmlMapperTest {

    @Test
    public void convertsRequestToJson() throws IOException {

        final StringEntity stringEntity = XacmlMapper.mapRequestToEntity(MockXacmlRequest.getXacmlRequest());

        assertThat(stringEntity.getContentLength(), greaterThan(0L));

        String expectedContentWithToken = getExpectedContentRequestWithToken();
        assertThat(Utils.entityToString(stringEntity), is(expectedContentWithToken));

        String expectedContentWithSubjectAttribute = getExpectedContentRequestWithSubjectAttribute();
        assertThat(Utils.entityToString(stringEntity), is(expectedContentWithSubjectAttribute));
    }

    private String getExpectedContentRequestWithToken() throws IOException {
        String expectedContent = Files.lines(get("C:\\Users\\e148211\\abac\\src\\test\\resources\\xacmlrequest-withtoken.json")).collect(Collectors.joining());
        return expectedContent;
    }

    private String getExpectedContentRequestWithSubjectAttribute() throws IOException {
        String expectedContent = Files.lines(get("C:\\Users\\e148211\\abac\\src\\test\\resources\\xacmlrequest-withsubjectattribute.json")).collect(Collectors.joining());
        return expectedContent;
    }
}