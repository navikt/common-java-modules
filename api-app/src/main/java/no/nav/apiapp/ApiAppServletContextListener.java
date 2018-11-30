package no.nav.apiapp;


import no.nav.apiapp.config.Konfigurator;
import no.nav.apiapp.feil.FeilMapper;
import no.nav.apiapp.metrics.PrometheusFilter;
import no.nav.apiapp.metrics.PrometheusServlet;
import no.nav.apiapp.rest.NavCorsFilter;
import no.nav.apiapp.rest.RestApplication;
import no.nav.apiapp.rest.SwaggerResource;
import no.nav.apiapp.rest.SwaggerUIServlet;
import no.nav.apiapp.selftest.IsAliveServlet;
import no.nav.apiapp.selftest.IsReadyServlet;
import no.nav.apiapp.selftest.SelfTestServlet;
import no.nav.apiapp.selftest.impl.LedigDiskPlassHelsesjekk;
import no.nav.apiapp.selftest.impl.OpenAMHelsesjekk;
import no.nav.apiapp.selftest.impl.STSHelsesjekk;
import no.nav.apiapp.selftest.impl.TruststoreHelsesjekk;
import no.nav.apiapp.soap.SoapServlet;
import no.nav.apiapp.util.JbossUtil;
import no.nav.brukerdialog.security.jaspic.OidcAuthModule;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig;
import no.nav.brukerdialog.security.oidc.provider.AzureADB2CProvider;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.brukerdialog.security.pingable.IssoIsAliveHelsesjekk;
import no.nav.brukerdialog.security.pingable.IssoSystemBrukerTokenHelsesjekk;
import no.nav.common.auth.LoginFilter;
import no.nav.common.auth.LoginProvider;
import no.nav.common.auth.openam.sbs.OpenAMLoginFilter;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.log.ContextDiscriminator;
import no.nav.log.LogFilter;
import no.nav.log.LoginfoServlet;
import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.web.security.DisableCacheHeadersFilter;
import no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestService;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.sbl.util.LogUtils;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.*;
import java.util.stream.Collectors;

import static ch.qos.logback.classic.Level.INFO;
import static java.util.Collections.emptyList;
import static no.nav.apiapp.ServletUtil.*;
import static no.nav.apiapp.soap.SoapServlet.soapTjenesterEksisterer;
import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static no.nav.brukerdialog.security.Constants.hasRedirectUrl;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.T;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.isEnvironmentClass;
import static no.nav.sbl.util.LogUtils.setGlobalLogLevel;
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
    public static final String INTERNAL_IS_READY = "/internal/isReady";
    public static final String INTERNAL_SELFTEST = "/internal/selftest";
    public static final String INTERNAL_METRICS = "/internal/metrics";
    public static final String SWAGGER_PATH = "/internal/swagger/";
    public static final String LOGINFO_PATH = "/internal/loginfo";
    public static final String WEBSERVICE_PATH = "/ws/*";

    public static final List<String> DEFAULT_PUBLIC_PATHS = Arrays.asList(
            "/internal/.*",
            "/ws/.*",
            "/api/ping"
    );

    private ContextLoaderListener contextLoaderListener = new ContextLoaderListener();

    private final Konfigurator konfigurator;

    // på jboss
    public ApiAppServletContextListener() {
        this(null);
    }

    // på jetty
    ApiAppServletContextListener(Konfigurator konfigurator) {
        this.konfigurator = konfigurator;
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
        konfigurerLogging(servletContext, apiApplication);

        leggTilFilter(servletContextEvent, new DisableCacheHeadersFilter(DisableCacheHeadersFilter.Config.builder()
                .allowClientStorage(true)
                .build()
        ));

        // Slik at man husker å fjerne constrains fra web.xml
        ConstraintSecurityHandler currentSecurityHandler = (ConstraintSecurityHandler) ConstraintSecurityHandler.getCurrentSecurityHandler();
        List<ConstraintMapping> constraintMappings = currentSecurityHandler != null ? currentSecurityHandler.getConstraintMappings() : emptyList();
        if (constraintMappings.size() > 2) { // Jetty setter opp 2 contraints by default
            List<Constraint> constraints = constraintMappings.subList(2, constraintMappings.size()).stream().map(ConstraintMapping::getConstraint).collect(Collectors.toList());
            throw new IllegalStateException("api-apper bruker ikke container-login lenger, men setter istede opp " + LoginFilter.class.getName() + ". Vennligst fjern security constraints fra web.xml: " + constraints);
        }

        // Automatisk oppsett av sikkerhet på jboss
        if (!(apiApplication instanceof ApiApplication.NaisApiApplication)) {

            JbossUtil.getJbossSecurityDomain().ifPresent(securityDomain -> {
                if (!"other".equals(securityDomain)) {
                    throw new IllegalStateException("api-apper bruker ikke container-login lenger, men setter opp istede " + LoginFilter.class.getName() + ". Vennligst fjern security-domain: " + securityDomain);
                }
            });

            List<LoginProvider> providers = new ArrayList<>();

            if (detectAndLogProperty(OpenAmConfig.OPENAM_RESTURL)) {
                OpenAmConfig openAmConfig = OpenAmConfig.fromSystemProperties();
                providers.add(new OpenAMLoginFilter(openAmConfig));
                leggTilBonne(servletContextEvent, new OpenAMHelsesjekk(openAmConfig));
            }

            List<OidcProvider> oidcProviders = new ArrayList<>();
            if (issoBrukes()) {
                oidcProviders.add(new IssoOidcProvider());
            }

            if (detectAndLogProperty(AzureADB2CConfig.AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME_SKYA)){
                oidcProviders.add(new AzureADB2CProvider(AzureADB2CConfig.readFromSystemProperties()));
            }

            if(!oidcProviders.isEmpty()){
                providers.add(new OidcAuthModule(oidcProviders));
            }

            leggTilFilter(servletContextEvent, new LoginFilter(providers, DEFAULT_PUBLIC_PATHS));
        }

        if (konfigurator != null) {
            konfigurator.getSpringBonner().forEach(b -> leggTilBonne(servletContextEvent, b));
        }

        leggTilFilter(servletContextEvent, PrometheusFilter.class);
        leggTilFilter(servletContextEvent, new LogFilter(FeilMapper::visDetaljer));
        leggTilFilter(servletContextEvent, NavCorsFilter.class);
        leggTilFilter(servletContextEvent, XFrameOptionsFilter.class);

        FilterRegistration.Dynamic characterEncodingRegistration = leggTilFilter(servletContextEvent, CharacterEncodingFilter.class);
        characterEncodingRegistration.setInitParameter("encoding", "UTF-8");
        characterEncodingRegistration.setInitParameter("forceEncoding", "true");

        WebApplicationContext webApplicationContext = getSpringContext(servletContextEvent);
        SelfTestService selfTestService = new SelfTestService(resolvePingables(webApplicationContext, konfigurator));

        leggTilServlet(servletContextEvent, IsAliveServlet.class, INTERNAL_IS_ALIVE);
        leggTilServlet(servletContextEvent, new IsReadyServlet(selfTestService), INTERNAL_IS_READY);
        leggTilServlet(servletContextEvent, new SelfTestServlet(selfTestService), INTERNAL_SELFTEST);
        leggTilServlet(servletContextEvent, new PrometheusServlet(selfTestService), INTERNAL_METRICS);
        leggTilServlet(servletContextEvent, new SwaggerUIServlet(apiApplication), SWAGGER_PATH + "*");
        leggTilServlet(servletContextEvent, LoginfoServlet.class, LOGINFO_PATH);

        settOppRestApi(servletContextEvent, apiApplication);
        if (soapTjenesterEksisterer(servletContext)) {
            leggTilServlet(servletContextEvent, new SoapServlet(), WEBSERVICE_PATH);
        }

        if (apiApplication instanceof ApiApplication.NaisApiApplication) {
            MetricsClient.enableMetrics(MetricsConfig.resolveNaisConfig());
        }

        LOGGER.info("contextInitialized - slutt");
        apiApplication.startup(servletContext);
    }

    private Collection<? extends Pingable> resolvePingables(WebApplicationContext webApplicationContext, Konfigurator konfigurator) {
        List<Pingable> pingables = new ArrayList<>();
        pingables.addAll(webApplicationContext.getBeansOfType(Pingable.class).values());
        if (konfigurator != null) {
            pingables.addAll(konfigurator.getPingables());
        }
        return pingables;
    }

    private boolean detectAndLogProperty(String propertyName) {
        Optional<String> optionalProperty = EnvironmentUtils.getOptionalProperty(propertyName);
        LOGGER.info("checking property: {} = {}", propertyName, optionalProperty.orElse(""));
        return optionalProperty.isPresent();
    }

    private boolean stsBrukes(ApiApplication apiApplication) {
        if (apiApplication instanceof ApiApplication.NaisApiApplication) {
            return getOptionalProperty(StsSecurityConstants.STS_URL_KEY).isPresent();
        } else {
            return apiApplication.brukSTSHelsesjekk();
        }
    }

    private boolean issoBrukes() {
        boolean harIssoLogin = konfigurator != null && konfigurator.harIssoLogin();
        boolean autoRegistration = hasRedirectUrl();
        LOGGER.info("isso? harIssoLogin={} auto={}", harIssoLogin, autoRegistration);
        return harIssoLogin || autoRegistration;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().invalidate();
        throw new IllegalStateException("api-apps should be stateless");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

    }

    private void konfigurerLogging(ServletContext servletContext, ApiApplication apiApplication) {
        ContextDiscriminator.setContextName(getContextName(servletContext, apiApplication));
    }

    // brukes til å separere forskjellige contexter deployet på samme jboss
    // f.eks. veilarbaktivitet og veilarbaktivitet-ws
    static String getContextName(ServletContext servletContext, ApiApplication apiApplication) {
        String contextPath = servletContext.getContextPath();
        return contextPath != null && contextPath.length() > 1 ? contextPath.substring(1) : EnvironmentUtils.requireApplicationName();
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
        leggTilBonne(servletContextEvent, new TruststoreHelsesjekk());
        if (issoBrukes()) {
            leggTilBonne(servletContextEvent, new IssoSystemBrukerTokenHelsesjekk());
            leggTilBonne(servletContextEvent, new IssoIsAliveHelsesjekk());
        }
        if (stsBrukes(apiApplication)) {
            leggTilBonne(servletContextEvent, new STSHelsesjekk());
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