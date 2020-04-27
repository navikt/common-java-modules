package no.nav.common.abac;


import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.exception.PepException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbacUtilsTest {

    private static final String TOKEN_BODY = "bb6--bbb";

    private static final String TOKEN = "aa-aa_aa4aa." + TOKEN_BODY + ".ccccc-c_88c";

    @Test
    public void girRiktigTokenBodyGittHeltToken() throws PepException {
        final String token = AbacUtils.extractOidcTokenBody(TOKEN);
        Assertions.assertThat(token).isEqualTo(TOKEN_BODY);
    }

    @Test
    public void girRiktigTokenBodyGittBody() throws PepException {
        final String token = AbacUtils.extractOidcTokenBody(TOKEN_BODY);
        Assertions.assertThat(token).isEqualTo(TOKEN_BODY);
    }

    @Test
    public void shouldReturnRequestedAttribute() {
        Resource resource = new Resource();
        List<Attribute> attributes = resource.getAttribute();

        attributes.add(new Attribute("attributeID1","attributeValue1"));
        attributes.add(new Attribute("attributeID2","attributeValue2"));

        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(resource));

        assertThat(AbacUtils.getResourceAttribute(request, "attributeID1"), is("attributeValue1"));
    }

    @Test
    public void shouldReturnEmptyIfRequestedAttributeIsNotFound() {
        Resource resource = new Resource();
        List<Attribute> attributes = resource.getAttribute();

        attributes.add(new Attribute("attributeID1","attributeValue1"));
        attributes.add(new Attribute("attributeID2","attributeValue2"));

        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(resource));

        assertThat(AbacUtils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnEmptyIfAttributeListIsEmpty() {
        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(new Resource()));

        assertThat(AbacUtils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnAmptyWhenResourceIsNotDefined() {
        XacmlRequest request = new XacmlRequest().withRequest(new Request());

        assertThat(AbacUtils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnAmptyWhenRequestIsNotDefined() {
        XacmlRequest request = new XacmlRequest();

        assertThat(AbacUtils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

}

