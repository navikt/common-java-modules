package no.nav.apiapp.servlet;

import org.eclipse.jetty.server.Dispatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FailingServletExample extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute(Dispatcher.ERROR_EXCEPTION, new IllegalStateException());
        resp.sendError(418, "fail fail fail");
    }

}
