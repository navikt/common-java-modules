package no.nav.common.health;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

public class HealthCheckServlet extends HttpServlet {

    private final List<HealthCheck> checks;

    public HealthCheckServlet(List<HealthCheck> checks) {
        this.checks = checks;
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Optional<HealthCheckResult> maybeFailedCheck = HealthChecker.findFirstFailingCheck(checks);

        if (maybeFailedCheck.isPresent()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

}
