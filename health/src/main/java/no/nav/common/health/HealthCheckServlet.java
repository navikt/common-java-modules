package no.nav.common.health;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Optional;

public class HealthCheckServlet extends HttpServlet {

    private final List<HealthCheck> checks;

    public HealthCheckServlet(List<HealthCheck> checks) {
        this.checks = checks;
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Optional<HealthCheckResult> maybeFailedCheck = HealthCheckUtils.findFirstFailingCheck(checks);

        if (maybeFailedCheck.isPresent()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

}
