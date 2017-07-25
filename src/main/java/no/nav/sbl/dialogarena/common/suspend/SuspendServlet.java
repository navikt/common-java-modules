package no.nav.sbl.dialogarena.common.suspend;

import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.slf4j.LoggerFactory.getLogger;

public class SuspendServlet extends HttpServlet {

    public static final long SIGNAL_READY_FOR_SHUTDOWN_IN_MS = 3000L;

    public enum ApplicationStatus {
        RUNNING, TO_BE_SUSPENDED, SUSPENDED;
    }

    private static final Logger LOGGER = getLogger(SuspendServlet.class);
    private static ApplicationStatus status = ApplicationStatus.RUNNING;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOGGER.info("[GET] Suspend-request mottatt - appen er i status {} ", status);
        if (status.equals(ApplicationStatus.SUSPENDED)) {
            response.setStatus(SC_OK);
        } else {
            response.sendError(SC_SERVICE_UNAVAILABLE, String.format("Service soknadaap er i status %s", status) );
        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        if (status.equals(ApplicationStatus.RUNNING)) {
            LOGGER.info("[PUT] Suspend-request mottat - starter å klargjøre for shutdown");
            status = ApplicationStatus.TO_BE_SUSPENDED;
            scheduleReadyForShutdown(SIGNAL_READY_FOR_SHUTDOWN_IN_MS);
        }
        response.setStatus(SC_OK);
    }

    private void scheduleReadyForShutdown(final long waitForShutdownMs) {
        Timer timer = new Timer("TimerTask - Venter ut eksisterende connections");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                status = ApplicationStatus.SUSPENDED;
                LOGGER.info("Ventet {} ms - suspended og klar for shutdown", waitForShutdownMs);
            }
        }, waitForShutdownMs);
    }

    public static boolean isRunning() {
        return status.equals(ApplicationStatus.RUNNING);
    }
}
