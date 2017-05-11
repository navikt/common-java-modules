package no.nav.sbl.dialogarena.common.abac;


import no.nav.sbl.dialogarena.common.abac.pep.Utils;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Resource;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;



import java.util.List;

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

