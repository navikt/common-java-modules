package no.nav.common.abac;


import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.request.Resource;
import no.nav.common.abac.domain.request.XacmlRequest;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UtilsTest {

    @Test
    public void shouldReturnRequestedAttribute() {
        Resource resource = new Resource();
        List<Attribute> attributes = resource.getAttribute();

        attributes.add(new Attribute("attributeID1","attributeValue1"));
        attributes.add(new Attribute("attributeID2","attributeValue2"));

        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(resource));

        assertThat(Utils.getResourceAttribute(request, "attributeID1"), is("attributeValue1"));
    }

    @Test
    public void shouldReturnEmptyIfRequestedAttributeIsNotFound() {
        Resource resource = new Resource();
        List<Attribute> attributes = resource.getAttribute();

        attributes.add(new Attribute("attributeID1","attributeValue1"));
        attributes.add(new Attribute("attributeID2","attributeValue2"));

        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(resource));

        assertThat(Utils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnEmptyIfAttributeListIsEmpty() {
        XacmlRequest request = new XacmlRequest().withRequest(new Request().withResource(new Resource()));

        assertThat(Utils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnAmptyWhenResourceIsNotDefined() {
        XacmlRequest request = new XacmlRequest().withRequest(new Request());

        assertThat(Utils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

    @Test
    public void shouldReturnAmptyWhenRequestIsNotDefined() {
        XacmlRequest request = new XacmlRequest();

        assertThat(Utils.getResourceAttribute(request, "attributeID3"), is("EMPTY"));
    }

}

