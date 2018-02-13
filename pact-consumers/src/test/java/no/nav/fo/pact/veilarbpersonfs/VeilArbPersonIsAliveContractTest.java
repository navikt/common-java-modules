package no.nav.fo.pact.veilarbpersonfs;

import no.nav.fo.pact.FOApplication;
import no.nav.fo.pact.global.IsAliveContract;

import java.util.Collections;

public class VeilArbPersonIsAliveContractTest extends IsAliveContract {

    public VeilArbPersonIsAliveContractTest() {
        super(FOApplication.VEILARBPERSONFS.getFoName(), FOApplication.VEILARBPERSON.getFoName());
        super.setHeaders(Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"));
    }

}
