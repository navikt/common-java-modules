package no.nav.sbl.dialogarena.common.abac.pep.domain;


import no.nav.abac.xacml.NavAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.Client;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Resource;

public class Resources {


    public static Resource makePersonResource(Client client) {

        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, client.getDomain()));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, client.getFnr()));
        return resource;

    }

    public static Resource makeKode7Resource(Client client) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, client.getDomain()));
        return resource;

    }

    public static Resource makeKode6Resource(Client client) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, client.getDomain()));
        return resource;
    }

    public static Resource makeEgenAnsattResource(Client client) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, client.getDomain()));
        return resource;
    }

}
