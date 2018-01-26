package no.nav.apiapp;


import no.nav.apiapp.log.ContextDiscriminator;
import no.nav.apiapp.log.LogUtils;
import no.nav.apiapp.logging.LoginfoServlet;
import no.nav.apiapp.metrics.PrometheusServlet;
import no.nav.apiapp.rest.NavCorsFilter;
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
import no.nav.sbl.util.EnvironmentUtils;
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

import static ch.qos.logback.classic.Level.INFO;
import static java.lang.System.getProperty;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static javax.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;
import static javax.servlet.SessionTrackingMode.COOKIE;
import static no.nav.apiapp.ApiApplication.Sone.SBS;
import static no.nav.apiapp.Constants.MILJO_PROPERTY_NAME;
import static no.nav.apiapp.ServletUtil.*;
import static no.nav.apiapp.config.Konfigurator.OPENAM_RESTURL;
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

    @Deprecated
    public static final String SPRING_CONTEKST_KLASSE_PARAMETER_NAME_DEPRECATED = "springContekstKlasse";

    public static final String SPRING_CONTEKST_KLASSE_PARAMETER_NAME = "springKontekstKlasse";
    private static final String SPRING_CONTEXT_KLASSENAVN = AnnotationConfigWebApplicationContext.class.getName();

    public static final String INTERNAL_IS_ALIVE = "/internal/isAlive";
    public static final String INTERNAL_SELFTEST = "/internal/selftest";
    public static final String INTERNAL_METRICS = "/internal/metrics";
    public static final String SWAGGER_PATH = "/internal/swagger/";
    public static final String LOGINFO_PATH = "/internal/loginfo";

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
        konfigurerSpring(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextDestroyed");
        ServletContext servletContext = servletContextEvent.getServletContext();
        if (!disablet(servletContext)) {
            getContext(servletContext).getBean(ApiApplication.class).shutdown(servletContext);
            contextLoaderListener.contextDestroyed(servletContextEvent);
            LogUtils.shutDownLogback();
        }
        LOGGER.info("contextDestroyed - slutt");
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextInitialized");
        ServletContext servletContext = servletContextEvent.getServletContext();
        if (disablet(servletContext)) {
            return;
        }

        if (!erSpringSattOpp(servletContextEvent)) {
            konfigurerSpring(servletContext);
        }
        ApiApplication apiApplication = startSpring(servletContextEvent);
        konfigurerLogging(apiApplication);

        if (skalHaOpenAm(apiApplication)) {
            leggTilFilter(servletContextEvent, OpenAMLoginFilter.class);
        }

        if (modigSecurityBrukes()) {
            leggTilFilter(servletContextEvent, MDCFilter.class);
        } else {
            // TODO hva ellers? - virker ikke som det finnes en dialogarena-ekvivalent.
        }

        leggTilFilter(servletContextEvent, NavCorsFilter.class);

        FilterRegistration.Dynamic characterEncodingRegistration = leggTilFilter(servletContextEvent, CharacterEncodingFilter.class);
        characterEncodingRegistration.setInitParameter("encoding", "UTF-8");
        characterEncodingRegistration.setInitParameter("forceEncoding", "true");

        leggTilServlet(servletContextEvent, IsAliveServlet.class, INTERNAL_IS_ALIVE);
        leggTilServlet(servletContextEvent, SelfTestServlet.class, INTERNAL_SELFTEST);
        leggTilServlet(servletContextEvent, PrometheusServlet.class, INTERNAL_METRICS);
        leggTilServlet(servletContextEvent, new SwaggerUIServlet(apiApplication), SWAGGER_PATH + "*");
        leggTilServlet(servletContextEvent, LoginfoServlet.class, LOGINFO_PATH);

        settOppRestApi(servletContextEvent, apiApplication);
        if (soapTjenesterEksisterer(servletContext)) {
            leggTilServlet(servletContextEvent, new SoapServlet(), "/ws/*");
        }
        settOppSessionOgCookie(servletContextEvent, apiApplication);
        LOGGER.info("contextInitialized - slutt");
        apiApplication.startup(servletContext);
    }

    private boolean skalHaOpenAm(ApiApplication apiApplication) {
        if (apiApplication instanceof ApiApplication.NaisApiApplication) {
            return EnvironmentUtils.getOptionalProperty(OPENAM_RESTURL).isPresent();
        } else {
            return apiApplication.getSone() == SBS;
        }
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

    private void konfigurerLogging(ApiApplication apiApplication) {
        ContextDiscriminator.setContextName(apiApplication.getApplicationName());
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
        String springKontekstKlasseNavn = servletContext.getInitParameter(SPRING_CONTEKST_KLASSE_PARAMETER_NAME);
        if (isEmpty(springKontekstKlasseNavn)) {
            springKontekstKlasseNavn = servletContext.getInitParameter(SPRING_CONTEKST_KLASSE_PARAMETER_NAME_DEPRECATED);
            if (!isEmpty(springKontekstKlasseNavn)) {
                LOGGER.warn("Appen din benytter 'springContekstKlasse' som context-param name. Denne er deprecated. Vennligst bytt til 'springKontekstKlasse' (skrivefeil rettet)");
            }
        }
        if (isEmpty(springKontekstKlasseNavn)) {
            throw new IllegalArgumentException(String.format("Vennligst oppgi din annoterte spring-kontekst-klasse som parameter '%s'", SPRING_CONTEKST_KLASSE_PARAMETER_NAME));
        }
        if (!erGyldigKlasse(springKontekstKlasseNavn)) {
            throw new IllegalArgumentException(String.format("Klassen '%s' er ikke en gyldig klasse", springKontekstKlasseNavn));
        }

        if (!erApiApplikasjon(springKontekstKlasseNavn)) {
            throw new IllegalArgumentException(String.format("Klassen '%s' må implementere %s", springKontekstKlasseNavn, ApiApplication.class));
        }
        return springKontekstKlasseNavn;
    }

    private boolean erApiApplikasjon(String springKontekstKlasseNavn) {
        try {
            return ApiApplication.class.isAssignableFrom(Class.forName(springKontekstKlasseNavn));
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
        if (apiApplication.brukSTSHelsesjekk()) {
            leggTilBonne(servletContextEvent, new STSHelsesjekk(apiApplication.getSone()));
        }
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

    private void settOppSessionOgCookie(ServletContextEvent servletContextEvent, ApiApplication apiApplication) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        String sessionCookieName = apiApplication.getApplicationName().toUpperCase() + "_JSESSIONID";

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

    private boolean disablet(ServletContext servletContext) {
        return of(servletContext.getInitParameter("disableApiApp")).map(Boolean::parseBoolean).orElse(false);
    }

}