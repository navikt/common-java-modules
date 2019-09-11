package no.nav.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.log.sbl.LogUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.log.sbl.LogUtils.getRootLevel;


public class LoginfoServlet extends HttpServlet{

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Logger> loggers = LogUtils.getAllLoggers();
        Level rootLevel = loggers.get(0).getLevel();

        String loggerAndLevel = loggers.stream()
                .filter(LoginfoServlet::differentThanRootLevel)
                .map(LoginfoServlet::createLogstring)
                .collect(Collectors.joining(""));

        resp.setContentType("text/html");
        resp.getWriter().write("<html><head></head><body>");
        resp.getWriter().write(String.format("<h1>ROOT log level: %s</h1>", rootLevel.toString()));
        resp.getWriter().write("<h1>Loggere med annen log level enn ROOT</h1>");
        resp.getWriter().write(loggerAndLevel);
        resp.getWriter().write("<body></html>");
    }

    private static boolean differentThanRootLevel(Logger logger) {
        return !logger.getEffectiveLevel().equals(getRootLevel());
    }

    private static String createLogstring(Logger logger) {
        return "<div>"+logger.getName()+ " - " + logger.getEffectiveLevel().toString()+"</div>";
    }

}
