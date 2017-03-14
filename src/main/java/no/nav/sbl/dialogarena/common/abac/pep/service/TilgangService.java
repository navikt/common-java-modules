package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;

import javax.naming.NamingException;
import java.io.IOException;

interface TilgangService {

    XacmlResponse askForPermission(XacmlRequest request) throws AbacException, NamingException, IOException, NoSuchFieldException;

}
