package no.nav.apiapp;

import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.springframework.web.context.support.WebApplicationContextUtils.findWebApplicationContext;

public class ServletUtil {

    public static String getApplicationName(ServletContext servletContext) {
        return servletContext.getContextPath().substring(1);
    }

    public static WebApplicationContext getContext(ServletContext servletContext) {
        return findWebApplicationContext(servletContext);
    }

    public static WebApplicationContext getContext(ServletContextEvent servletContextEvent) {
        return getContext(servletContextEvent.getServletContext());
    }

}
