package no.nav.apiapp.rest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.API_PATH;

public class SwaggerUIServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String etterspurtFil = request.getRequestURI().substring(request.getContextPath().length() + request.getServletPath().length());
        if ("".equals(etterspurtFil)) {
            response.sendRedirect("./swagger/");
        } else if ("/".equals(etterspurtFil)) {
            response.sendRedirect("./index.html?input_baseurl=" + request.getContextPath() + API_PATH + SwaggerResource.SWAGGER_JSON);
        } else {
            RequestDispatcher indrequestDispatcherx = getServletContext().getRequestDispatcher("/webjars/swagger-ui/2.2.10" + etterspurtFil);
            indrequestDispatcherx.include(request, response);
        }
    }

}