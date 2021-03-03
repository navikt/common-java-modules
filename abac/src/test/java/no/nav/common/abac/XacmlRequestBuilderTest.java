package no.nav.common.abac;

import no.nav.common.abac.domain.Attribute;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import org.junit.Test;

import static no.nav.common.abac.constants.NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE;
import static no.nav.common.abac.constants.NavAttributter.RESOURCE_FELLES_PERSON_FNR;
import static org.junit.Assert.assertEquals;

public class XacmlRequestBuilderTest {

    @Test
    public void personIdAttribute_skal_lage_attribute_for_fnr() {
        EksternBrukerId id = Fnr.of("test");
        Attribute attribute = XacmlRequestBuilder.personIdAttribute(id);

        assertEquals(RESOURCE_FELLES_PERSON_FNR, attribute.getAttributeId());
        assertEquals(id.get(), attribute.getValue());
    }

    @Test
    public void personIdAttribute_skal_lage_attribute_for_norsk_ident() {
        EksternBrukerId id = NorskIdent.of("test");
        Attribute attribute = XacmlRequestBuilder.personIdAttribute(id);

        assertEquals(RESOURCE_FELLES_PERSON_FNR, attribute.getAttributeId());
        assertEquals(id.get(), attribute.getValue());
    }

    @Test
    public void personIdAttribute_skal_lage_attribute_for_aktor_id() {
        EksternBrukerId id = AktorId.of("test");
        Attribute attribute = XacmlRequestBuilder.personIdAttribute(id);

        assertEquals(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, attribute.getAttributeId());
        assertEquals(id.get(), attribute.getValue());
    }

}
