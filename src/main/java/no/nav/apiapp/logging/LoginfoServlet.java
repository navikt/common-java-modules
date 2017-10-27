package no.nav.apiapp.logging;

import ch.qos.logback.classic.Logger;
import no.nav.apiapp.log.LogUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class LoginfoServlet extends HttpServlet{

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Logger> loggers = LogUtils.getAllLoggers();
        String loggerAndLevel = loggers.stream().map(LoginfoServlet::createLogstring).collect(Collectors.joining(""));
        resp.setContentType("text/html");
        resp.getWriter().write("<h1>Liste over alle loggere og loglevel</h1>");
        resp.getWriter().write(loggerAndLevel);
    }

    private static String createLogstring(Logger logger) {
        return "<div>"+logger.getName()+ " - " + logger.getEffectiveLevel().toString()+"</div>";
    }

}
