package no.nav.common.health.selftest;

import no.nav.common.utils.EnvironmentUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.aggregateStatus;
import static no.nav.common.health.selftest.SelfTestUtils.checkAll;

public class SelftTestServlet extends HttpServlet {

    private final List<SelfTestCheck> checks;

    public SelftTestServlet(List<SelfTestCheck> checks) {
        this.checks = checks;
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String host = EnvironmentUtils.resolveHostName();
        LocalDateTime now = LocalDateTime.now();

        List<SelftTestCheckResult> checkResults = checkAll(checks);
        String html = SelftestHtmlGenerator.generate(checkResults, host, now);
        int status = aggregateStatus(checkResults) == SelfTestStatus.ERROR ? 500 : 200;

        resp.setStatus(status);
        resp.setContentType("text/html");
        resp.getWriter().write(html);
    }

}
