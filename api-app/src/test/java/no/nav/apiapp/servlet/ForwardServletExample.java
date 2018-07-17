package no.nav.apiapp.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.INTERNAL_IS_ALIVE;

public class ForwardServletExample extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(INTERNAL_IS_ALIVE).forward(req, resp);
    }

}
