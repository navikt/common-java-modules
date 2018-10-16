package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus.OK;

public class IsReadyServlet extends HttpServlet {

    private final SelfTestService selfTestService;

    private volatile boolean ready;

    public IsReadyServlet(SelfTestService selfTestService) {
        this.selfTestService = selfTestService;
    }

    @Override
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        if (!ready) {
            ready = selfTestService.selfTest().getAggregateResult() == OK;
        }

        httpServletResponse.setStatus(ready ? SC_OK : SC_SERVICE_UNAVAILABLE);
        httpServletResponse.setContentType("text/plain");
        httpServletResponse.getWriter().write(ready ? "READY" : "NOT READY");
    }

}
