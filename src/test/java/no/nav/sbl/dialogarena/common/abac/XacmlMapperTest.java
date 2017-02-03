package no.nav.sbl.dialogarena.common.abac;

import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import java.io.IOException;

import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


public class XacmlMapperTest {

    @Test
    public void convertsRequestToJson() throws IOException {

        final StringEntity stringEntity = XacmlMapper.mapRequestToEntity(MockXacmlRequest.getXacmlRequest());

        assertThat(stringEntity.getContentLength(), greaterThan(0L));

        String expectedContent = getContentFromJsonFile("xacmlrequest-withtoken.json");
        assertThat(Utils.entityToString(stringEntity), is(expectedContent));
    }
}