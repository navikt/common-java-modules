package no.nav.sbl.dialogarena.common.jetty;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Map;

import static java.util.Collections.list;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@WebListener
public class TestListener implements ServletContextListener {

    private static Map<String, String> contextParams;


    public static Map<String, String> getContextParams() {
        return contextParams;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        contextParams = list(sce.getServletContext().getInitParameterNames())
                .stream()
                .collect(toMap(identity(), identity()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
