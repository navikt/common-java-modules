package no.nav.sbl.dialogarena.common.suspend;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

public class IsAliveServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(!SuspendServlet.isRunning()){
            response.sendError(SC_SERVICE_UNAVAILABLE, "Service is suspended for application update");
        }
        else{
            response.getWriter().write("{status : \"ok\", message: \"AAP fungerer\"}");
        }
    }
}
