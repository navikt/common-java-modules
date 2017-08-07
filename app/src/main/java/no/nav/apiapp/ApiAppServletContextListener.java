package no.nav.apiapp;


import no.nav.apiapp.log.ContextDiscriminator;
import no.nav.apiapp.log.LogUtils;
import no.nav.apiapp.rest.RestApplication;
import no.nav.apiapp.rest.SwaggerResource;
import no.nav.apiapp.rest.SwaggerUIServlet;
import no.nav.apiapp.selftest.IsAliveServlet;
import no.nav.apiapp.selftest.SelfTestServlet;
import no.nav.apiapp.selftest.impl.LedigDiskPlassHelsesjekk;
import no.nav.apiapp.selftest.impl.STSHelsesjekk;
import no.nav.apiapp.soap.SoapServlet;
import no.nav.apiapp.util.JbossUtil;
import no.nav.brukerdialog.security.pingable.IssoIsAliveHelsesjekk;
import no.nav.brukerdialog.security.pingable.IssoSystemBrukerTokenHelsesjekk;
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

import static ch.qos.logback.classic.Level.INFO;
import static java.lang.System.getProperty;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static javax.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;
import static javax.servlet.SessionTrackingMode.COOKIE;
import static no.nav.apiapp.ApiApplication.Sone.SBS;
import static no.nav.apiapp.Constants.MILJO_PROPERTY_NAME;
import static no.nav.apiapp.ServletUtil.*;
import static no.nav.apiapp.log.LogUtils.setGlobalLogLevel;
import static no.nav.apiapp.soap.SoapServlet.soapTjenesterEksisterer;
import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAppServletContextListener.class);

    public static final String SPRING_CONTEKST_KLASSE_PARAMETER_NAME = "springContekstKlasse";
    private static final String SPRING_CONTEXT_KLASSENAVN = AnnotationConfigWebApplicationContext.class.getName();

    public static final String INTERNAL_IS_ALIVE = "/internal/isAlive";
    public static final String INTERNAL_SELFTEST = "/internal/selftest";
    public static final String SWAGGER_PATH = "/internal/swagger/";

    private ContextLoaderListener contextLoaderListener = new ContextLoaderListener();

    private int sesjonsLengde;

    static {
        if (getProperty(MILJO_PROPERTY_NAME, "").equals("t")) {
            setGlobalLogLevel(INFO);
        }
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        LOGGER.info("onStartup");
        if (!disablet(servletContext)) {
            konfigurerLogging(servletContext);
            konfigurerSpring(servletContext);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextDestroyed");
        if (!disablet(servletContextEvent.getServletContext())) {
            contextLoaderListener.contextDestroyed(servletContextEvent);
            LogUtils.shutDownLogback();
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextInitialized");
        ServletContext servletContext = servletContextEvent.getServletContext();
        if (disablet(servletContext)) {
            return;
        }
        konfigurerLogging(servletContext);

        if (!erSpringSattOpp(servletContextEvent)) {
            konfigurerSpring(servletContext);
        }
        ApiApplication apiApplication = startSpring(servletContextEvent);

        if (apiApplication.getSone() == SBS) {
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
        leggTilServlet(servletContextEvent, new SwaggerUIServlet(apiApplication), SWAGGER_PATH + "*");

        settOppRestApi(servletContextEvent, apiApplication);
        if (soapTjenesterEksisterer(servletContext)) {
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

    private boolean issoBrukes() {
        boolean jaspiAuthProvider = getProperty(DEFAULT_FACTORY_SECURITY_PROPERTY, "").contains("jaspi"); // på jetty
        boolean autoRegistration = JbossUtil.brukerJaspi(); // på jboss
        LOGGER.info("isso? jaspi={} auto={}", jaspiAuthProvider, autoRegistration);
        return jaspiAuthProvider || autoRegistration;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().setMaxInactiveInterval(sesjonsLengde);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

    }

    private void konfigurerLogging(ServletContext servletContext) {
        ContextDiscriminator.setContextName(getApplicationName(servletContext));
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

        if (!erApiApplikasjon(springContekstKlasseNavn)) {
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
        AnnotationConfigWebApplicationContext webApplicationContext = getSpringContext(servletContextEvent);
        ApiApplication apiApplication = webApplicationContext.getBean(ApiApplication.class);
        leggTilBonne(servletContextEvent, new LedigDiskPlassHelsesjekk());
        if (issoBrukes()) {
            leggTilBonne(servletContextEvent, new IssoSystemBrukerTokenHelsesjekk());
            leggTilBonne(servletContextEvent, new IssoIsAliveHelsesjekk());
        }
        leggTilBonne(servletContextEvent, new STSHelsesjekk(apiApplication.getSone()));
        return apiApplication;
    }

    private void leggTilBonne(ServletContextEvent servletContextEvent, Object bonne) {
        getSpringContext(servletContextEvent).getBeanFactory().registerSingleton(bonne.getClass().getName(), bonne);
    }

    private void settOppRestApi(ServletContextEvent servletContextEvent, ApiApplication apiApplication) {
        RestApplication restApplication = new RestApplication(getContext(servletContextEvent.getServletContext()), apiApplication);
        ServletContainer servlet = new ServletContainer(ResourceConfig.forApplication(restApplication));
        ServletRegistration.Dynamic servletRegistration = leggTilServlet(servletContextEvent, servlet, sluttMedSlash(apiApplication.getApiBasePath()) + "*");
        SwaggerResource.setupServlet(servletRegistration);
    }

    private void settOppSessionOgCookie(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        String sessionCookieName = getApplicationName(servletContext).toUpperCase() + "_JSESSIONID";

        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setSecure(true);
        LOGGER.info("SessionCookie: {}", sessionCookieName);
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

    private boolean disablet(ServletContext servletContext) {
        return of(servletContext.getInitParameter("disableApiApp")).map(Boolean::parseBoolean).orElse(false);
    }

}