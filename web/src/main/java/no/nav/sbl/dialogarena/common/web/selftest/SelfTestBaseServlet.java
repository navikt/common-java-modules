package no.nav.sbl.dialogarena.common.web.selftest;

import lombok.SneakyThrows;
import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.generators.SelftestHtmlGenerator;
import no.nav.sbl.dialogarena.common.web.selftest.generators.SelftestJsonGenerator;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.util.sbl.EnvironmentUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public abstract class SelfTestBaseServlet extends HttpServlet {


    private SelfTestService selfTestService;

    /**
     * @param pingables Collection av alle tjenester som skal inngå i selftesten. Tjenestene må implementere Pingable-grensesnittet.
     */
    protected SelfTestBaseServlet(Collection<? extends Pingable> pingables) {
        setPingables(pingables);
    }

    protected SelfTestBaseServlet(SelfTestService selfTestService) {
        setSelfTestService(selfTestService);
    }

    protected void setPingables(Collection<? extends Pingable> pingables) {
        setSelfTestService(new SelfTestService(pingables));
    }

    protected void setSelfTestService(SelfTestService selfTestService) {
        this.selfTestService = selfTestService;
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Selftest selftest = selfTestService.selfTest();

        if ("application/json".equalsIgnoreCase(req.getHeader("accept"))) {
            resp.setContentType("application/json");
            resp.getWriter().write(SelftestJsonGenerator.generate(selftest));
        } else {
            resp.setContentType("text/html");
            resp.getWriter().write(SelftestHtmlGenerator.generate(selftest, getHost()));
        }
    }

    @SneakyThrows
    protected String getHost() {
        return EnvironmentUtils.resolveHostName();
    }

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_WARNING = 2;
    public static final int STATUS_AVSKRUDD = 3;

    public static int statusToCode(SelfTestStatus selfTestStatus) {
        if (selfTestStatus == null) {
            return STATUS_ERROR;
        }
        switch (selfTestStatus) {
            case OK:
                return STATUS_OK;
            case WARNING:
                return STATUS_WARNING;
            case DISABLED:
                return STATUS_AVSKRUDD;
            default:
                return STATUS_ERROR;
        }
    }

}
