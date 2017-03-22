package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import java.io.IOException;

interface LdapTilgangService {

    XacmlResponse askForPermission() throws AbacException, PepException, IOException, NoSuchFieldException;

}
