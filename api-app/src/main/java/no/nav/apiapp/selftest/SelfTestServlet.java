package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import java.util.Collection;

import static no.nav.apiapp.ServletUtil.getContext;

public class SelfTestServlet extends SelfTestBaseServlet {

    public SelfTestServlet(SelfTestService selfTestService) {
        super(selfTestService);
    }

}