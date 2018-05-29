package no.nav.apiapp.rest;

import no.nav.apiapp.ApiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.SWAGGER_PATH;
import static no.nav.apiapp.rest.SwaggerResource.SWAGGER_JSON;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;

public class SwaggerUIServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerUIServlet.class);

    private final ApiApplication apiApplication;

    public SwaggerUIServlet(ApiApplication apiApplication) {
        this.apiApplication = apiApplication;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String etterspurtFil = request.getRequestURI().substring(request.getContextPath().length() + request.getServletPath().length());
        if ("".equals(etterspurtFil)) {
            response.sendRedirect("./swagger/");
        } else if ("/".equals(etterspurtFil)) {
            response.sendRedirect("./index.html?input_baseurl=" + request.getContextPath() + SWAGGER_PATH + SWAGGER_JSON);
        } else if (("/" + SWAGGER_JSON).equals(etterspurtFil)) {
            dispatch(sluttMedSlash(apiApplication.getApiBasePath()) + SWAGGER_JSON, request, response);
        } else {
            dispatch("/webjars/swagger-ui/2.2.10" + etterspurtFil, request, response);
        }
    }

    private void dispatch(String path, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOGGER.info("forward: [{}] -> [{}]", request.getRequestURI(), path);
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher(path);
        requestDispatcher.forward(request, response);
    }

}