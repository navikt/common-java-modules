package no.nav.apiapp;


import no.nav.apiapp.rest.RestApplication;
import no.nav.apiapp.selftest.IsAliveServlet;
import no.nav.apiapp.selftest.SelfTestJsonServlet;
import no.nav.apiapp.selftest.SelfTestServlet;
import no.nav.apiapp.soap.SoapServlet;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.presentation.logging.session.MDCFilter;
import no.nav.modig.security.filter.OpenAMLoginFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.EnumSet;

import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static javax.servlet.SessionTrackingMode.COOKIE;
import static no.nav.apiapp.ServletUtil.getApplicationName;
import static no.nav.apiapp.ServletUtil.getContext;
import static no.nav.apiapp.soap.SoapServlet.soapTjenesterEksisterer;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.web.context.ContextLoader.CONFIG_LOCATION_PARAM;
import static org.springframework.web.context.ContextLoader.CONTEXT_CLASS_PARAM;

/*
- Bruker @WebListener isteden for 'servletContext.addListener(this);'
  fordi JBOSS da oss å legge til servlets/filtere i contextInitialized() - UT010042

- må ha presedens over SpringWebApplicationInitializer, da vi ønsker å kontrollere initialiseringen av Spring!
 */
@WebListener
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiAppServletContextListener implements WebApplicationInitializer, ServletContextListener, HttpSessionListener {

    public static final String SPRING_CONTEKST_KLASSE_PARAMETER_NAME = "springContekstKlasse";
    private static final String SPRING_CONTEXT_KLASSENAVN = AnnotationConfigWebApplicationContext.class.getName();

    public static final String INTERNAL_IS_ALIVE = "/internal/isAlive";
    public static final String INTERNAL_SELFTEST = "/internal/selftest";
    public static final String INTERNAL_SELFTEST_JSON = "/internal/selftest.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAppServletContextListener.class);

    private ContextLoaderListener contextLoaderListener = new ContextLoaderListener();

    private int sesjonsLengde;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        LOGGER.info("onStartup");
        konfigurerSpring(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextDestroyed");
        contextLoaderListener.contextDestroyed(servletContextEvent);
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextInitialized");
        if(!erSpringSattOpp(servletContextEvent)){
            konfigurerSpring(servletContextEvent.getServletContext());
        }
        ApiApplication apiApplication = startSpring(servletContextEvent);

        if (apiApplication.getSone() == ApiApplication.Sone.SBS) {
            leggTilFilter(servletContextEvent, OpenAMLoginFilter.class);
        }

        if (modigSecurityBrukes()) {
            leggTilFilter(servletContextEvent, MDCFilter.class);
        } else {
            // TODO hva ellers? - virker ikke som det finnes en dialogarena-ekvivalent.
        }

        FilterRegistration.Dynamic characterEncodingRegistration = leggTilFilter(servletContextEvent, CharacterEncodingFilter.class);
        characterEncodingRegistration.setInitParameter("encoding", "UTF-8");
        characterEncodingRegistration.setInitParameter("forceEncoding", "true");

        leggTilServlet(servletContextEvent, IsAliveServlet.class, INTERNAL_IS_ALIVE);
        leggTilServlet(servletContextEvent, SelfTestServlet.class, INTERNAL_SELFTEST);
        leggTilServlet(servletContextEvent, SelfTestJsonServlet.class, INTERNAL_SELFTEST_JSON);

        settOppRestApi(servletContextEvent);
        if (soapTjenesterEksisterer(servletContextEvent.getServletContext())) {
            leggTilServlet(servletContextEvent, new SoapServlet(), "/ws/*");
        }
        settOppSessionOgCookie(servletContextEvent);
        LOGGER.info("contextInitialized - slutt");
    }

    private boolean modigSecurityBrukes() {
        try {
            return SubjectHandler.getSubjectHandler() instanceof SubjectHandler;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().setMaxInactiveInterval(sesjonsLengde);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

    }

    private void konfigurerSpring(ServletContext servletContext) {
        servletContext.setInitParameter(CONTEXT_CLASS_PARAM, SPRING_CONTEXT_KLASSENAVN);
        servletContext.setInitParameter(CONFIG_LOCATION_PARAM, getAppKontekstKlasseNavn(servletContext));

        // Se JettySubjectHandler. Strengt talt ikke nødvendig på JBOSS, men greit å ha mest mulig konsistens
        servletContext.addListener(RequestContextListener.class);
    }

    private boolean erSpringSattOpp(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        return SPRING_CONTEXT_KLASSENAVN.equals(servletContext.getInitParameter(CONTEXT_CLASS_PARAM))
                && getAppKontekstKlasseNavn(servletContext).equals(servletContext.getInitParameter(CONFIG_LOCATION_PARAM))
                ;
    }

    private String getAppKontekstKlasseNavn(ServletContext servletContext) {
        /////////////////
        // TODO validering ????
        /////////////////
        String springContekstKlasseNavn = servletContext.getInitParameter(SPRING_CONTEKST_KLASSE_PARAMETER_NAME);
        if (isEmpty(springContekstKlasseNavn)) {
            throw new IllegalArgumentException(String.format("Vennligst oppgi din annoterte spring-contekst-klasse som parameter '%s'", SPRING_CONTEKST_KLASSE_PARAMETER_NAME));
        }
        if (!erGyldigKlasse(springContekstKlasseNavn)) {
            throw new IllegalArgumentException(String.format("Klassen '%s' er ikke en gyldig klasse", springContekstKlasseNavn));
        }

        if(!erApiApplikasjon(springContekstKlasseNavn)){
            throw new IllegalArgumentException(String.format("Klassen '%s' må implementere %s", springContekstKlasseNavn, ApiApplication.class));
        }
        return springContekstKlasseNavn;
    }

    private boolean erApiApplikasjon(String springContekstKlasseNavn) {
        try {
            return ApiApplication.class.isAssignableFrom(Class.forName(springContekstKlasseNavn));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private ApiApplication startSpring(ServletContextEvent servletContextEvent) {
        contextLoaderListener.contextInitialized(servletContextEvent);
        return getContext(servletContextEvent.getServletContext()).getBean(ApiApplication.class);
    }

    private void settOppRestApi(ServletContextEvent servletContextEvent) {
        RestApplication restApplication = new RestApplication(getContext(servletContextEvent.getServletContext()));
        ServletContainer servlet = new ServletContainer(ResourceConfig.forApplication(restApplication));
        leggTilServlet(servletContextEvent, servlet, "/api/*");
    }

    private void settOppSessionOgCookie(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        String sessionCookieName = getApplicationName(servletContext).toUpperCase() + "_JSESSIONID";

        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setSecure(true);
        LOGGER.info("SessionCookie: {}",sessionCookieName);
        sessionCookieConfig.setName(sessionCookieName);
        servletContext.setSessionTrackingModes(singleton(COOKIE));
        sesjonsLengde = ofNullable(servletContextEvent.getServletContext().getInitParameter("maksSesjonsLengde"))
                .map(Integer::parseInt)
                .orElse(60);
    }

    private static boolean erGyldigKlasse(String klasseNavn) {
        try {
            return Class.forName(klasseNavn) != null;
        } catch (ClassNotFoundException e) {
            LOGGER.warn(e.getMessage(), e);
            return false;
        }
    }

    private static FilterRegistration.Dynamic leggTilFilter(ServletContextEvent servletContextEvent, Class<? extends Filter> filterClass) {
        FilterRegistration.Dynamic dynamic = servletContextEvent.getServletContext().addFilter(filterClass.getName(), filterClass);
        dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        LOGGER.info("la til filter [{}]", filterClass.getName());
        return dynamic;
    }

    private static void leggTilServlet(ServletContextEvent servletContextEvent, Class<? extends Servlet> servletClass, String path) {
        servletContextEvent.getServletContext().addServlet(servletClass.getName(), servletClass).addMapping(path);
        // TODO eksperimenter med setServletSecurity()!
        LOGGER.info("la til servlet [{}] på [{}]", servletClass.getName(), path);
    }

    private void leggTilServlet(ServletContextEvent servletContextEvent, Servlet servlet, String path) {
        servletContextEvent.getServletContext().addServlet(servlet.getClass().getName(), servlet).addMapping(path);
        LOGGER.info("la til servlet [{}] på [{}]", servlet, path);
    }

}