package no.nav.apiapp.rest;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.util.UrlUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.SWAGGER_PATH;
import static no.nav.apiapp.rest.SwaggerResource.SWAGGER_JSON;
import static no.nav.apiapp.util.UrlUtils.joinPaths;
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
            response.sendRedirect("./index.html?input_baseurl=" + joinPaths(request.getContextPath(), apiApplication.getApiBasePath(), SWAGGER_JSON));
        } else {
            dispatch("/webjars/swagger-ui/2.2.10" + etterspurtFil, request, response);
        }
    }

    private void dispatch(String path, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOGGER.info("forward: [{}] -> [{}]", request.getRequestURI(), path);

        // ensure we use the default file-resource-serving servlet of the servlet context. Phew
        RequestDispatcher requestDispatcher = getServletContext().getNamedDispatcher("default");
        // ensure the swagger-ui is served correctly, even when the actual path on disk differs from the request path
        requestDispatcher.forward(new SwaggerRequest(request, path), response);
    }

    private static class SwaggerRequest extends HttpServletRequestWrapper {

        private final String path;

        private SwaggerRequest(HttpServletRequest request, String path) {
            super(request);
            this.path = path;
        }

        @Override
        public String getServletPath() {
            return "/";
        }

        @Override
        public String getPathInfo() {
            return path;
        }
    }

}