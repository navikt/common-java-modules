package no.nav.apiapp.selftest;

import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static no.nav.apiapp.ServletUtil.getContext;

public class IsAliveServlet extends HttpServlet {

    private volatile ApplicationContext applicationContext;

    @Override
    public void init() throws ServletException {
        applicationContext = getContext(getServletContext());
        super.init();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean isUp = applicationContext.getStartupDate() > 0;
        resp.setStatus(isUp ? SC_OK : SC_SERVICE_UNAVAILABLE);
        resp.setContentType("text/plain");
        resp.getWriter().write("Application: " + (isUp ? "UP" : "DOWN"));
    }

}