package no.nav.sbl.dialogarena.common.web;


import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.test.path.FilesAndDirs.TEST_WEBAPP_SOURCE;
import static no.nav.sbl.dialogarena.test.path.FilesAndDirs.WEBAPP_SOURCE;

public final class StartJetty {

    public static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Jetty jetty = usingWar(WEBAPP_SOURCE).at("common").port(PORT).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private StartJetty() { }

}
