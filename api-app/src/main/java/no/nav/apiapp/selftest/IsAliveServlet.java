package no.nav.apiapp.selftest;

import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.apiapp.ServletUtil.getContext;

public class IsAliveServlet extends HttpServlet {

    private ApplicationContext ctx;

    @Override
    public void init() throws ServletException {
        ctx = getContext(getServletContext());
        super.init();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String status = ctx.getStartupDate() > 0 ? "UP" : "DOWN";
        resp.setContentType("text/html");
        resp.getWriter().write("Application: " + status);
    }
}
