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
import no.nav.apiapp.selftest.impl.STSHelsesjekk;
import no.nav.apiapp.selftest.impl.TruststoreHelsesjekk;
import no.nav.apiapp.soap.SoapServlet;
import no.nav.apiapp.version.Version;
import no.nav.apiapp.version.VersionService;
import no.nav.common.oidc.auth.OidcAuthenticationFilter;
import no.nav.log.LogFilter;
import no.nav.log.LogFilterConfig;
import no.nav.log.LoginfoServlet;
import no.nav.log.MarkerBuilder;
import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsConfig;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.web.security.DisableCacheHeadersFilter;
import no.nav.sbl.dialogarena.common.web.security.SecurityHeadersFilter;
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
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.RequestContextFilter;

import javax.servlet.*;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static javax.servlet.DispatcherType.REQUEST;
import static no.nav.apiapp.ServletUtil.*;
import static no.nav.apiapp.soap.SoapServlet.soapTjenesterEksisterer;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getPropertyAsBooleanOrElseFalse;
import static org.springframework.web.context.ContextLoader.CONFIG_LOCATION_PARAM;
import static org.springframework.web.context.ContextLoader.CONTEXT_CLASS_PARAM;

public class ApiAppServletContextListener implements ServletContextListener, HttpSessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAppServletContextListener.class);

    public static final String INTERNAL_IS_ALIVE = "/internal/isAlive";
    public static final String INTERNAL_IS_READY = "/internal/isReady";
    public static final String INTERNAL_SELFTEST = "/internal/selftest";
    public static final String INTERNAL_METRICS = "/internal/metrics";
    public static final String SWAGGER_PATH = "/internal/swagger/";
    public static final String LOGINFO_PATH = "/internal/loginfo";
    public static final String WEBSERVICE_PATH = "/ws/*";

    private final ContextLoaderListener contextLoaderListener = new ContextLoaderListener();
    private final ApiApplication apiApplication;
    private final Konfigurator konfigurator;

    ApiAppServletContextListener(Konfigurator konfigurator, ApiApplication apiApplication) {
        this.konfigurator = konfigurator;
        this.apiApplication = apiApplication;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextDestroyed");
        ServletContext servletContext = servletContextEvent.getServletContext();
        apiApplication.shutdown(servletContext);
        contextLoaderListener.contextDestroyed(servletContextEvent);
        LOGGER.info("contextDestroyed - slutt");
        LogUtils.shutDownLogback();
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info("contextInitialized");
        final VersionService versionService = new VersionService();
        List<Version> versions = versionService.getVersions();
        versions.forEach(v -> new MarkerBuilder().field("component", v.component).field("version", v.version).logInfo(LOGGER));

        ServletContext servletContext = servletContextEvent.getServletContext();

        konfigurerSpring(servletContext);
        WebApplicationContext webApplicationContext = startSpring(servletContextEvent);

        filterBuilder(new DisableCacheHeadersFilter(DisableCacheHeadersFilter.Config.builder()
                .allowClientStorage(getPropertyAsBooleanOrElseFalse("ALLOW_CLIENT_STORAGE"))
                .disablePragmaHeader(getPropertyAsBooleanOrElseFalse("DISABLE_PRAGMA_HEADER"))
                .build()
        )).register(servletContextEvent.getServletContext());

        // Slik at man husker å fjerne constrains fra web.xml
        ConstraintSecurityHandler currentSecurityHandler = (ConstraintSecurityHandler) ConstraintSecurityHandler.getCurrentSecurityHandler();
        List<ConstraintMapping> constraintMappings = currentSecurityHandler != null ? currentSecurityHandler.getConstraintMappings() : emptyList();
        if (constraintMappings.size() > 2) { // Jetty setter opp 2 contraints by default
            List<Constraint> constraints = constraintMappings.subList(2, constraintMappings.size()).stream().map(ConstraintMapping::getConstraint).collect(Collectors.toList());
            throw new IllegalStateException("api-apper bruker ikke container-login lenger, men setter istede opp " + OidcAuthenticationFilter.class.getName() + ". Vennligst fjern security constraints fra web.xml: " + constraints);
        }

        LogFilterConfig logFilterConfig = LogFilterConfig.builder()
                .exposeErrorDetails(FeilMapper::visDetaljer)
                .serverName(EnvironmentUtils.requireApplicationName())
                .build();

        filterBuilder(ComplianceEnforcementFilter.class)
                .dispatcherTypes(REQUEST)
                .urlPatterns(apiApplication.getApiBasePath() + "*")
                .register(servletContextEvent);

        filterBuilder(PrometheusFilter.class).register(servletContext);
        filterBuilder(new LogFilter(logFilterConfig)).register(servletContext);
        filterBuilder(NavCorsFilter.class).register(servletContext);
        filterBuilder(SecurityHeadersFilter.class).register(servletContext);
        filterBuilder(RequestContextFilter.class).register(servletContext);

        FilterRegistration.Dynamic characterEncodingRegistration =  filterBuilder(CharacterEncodingFilter.class)
                .register(servletContextEvent.getServletContext());
        characterEncodingRegistration.setInitParameter("encoding", "UTF-8");
        characterEncodingRegistration.setInitParameter("forceEncoding", "true");

        SelfTestService selfTestService = new SelfTestService(resolvePingables(webApplicationContext, konfigurator));

        leggTilServlet(servletContextEvent, IsAliveServlet.class, INTERNAL_IS_ALIVE);
        leggTilServlet(servletContextEvent, new IsReadyServlet(selfTestService), INTERNAL_IS_READY);
        leggTilServlet(servletContextEvent, new SelfTestServlet(selfTestService), INTERNAL_SELFTEST);
        leggTilServlet(servletContextEvent, new PrometheusServlet(selfTestService, versions), INTERNAL_METRICS);
        leggTilServlet(servletContextEvent, new SwaggerUIServlet(apiApplication), SWAGGER_PATH + "*");
        leggTilServlet(servletContextEvent, LoginfoServlet.class, LOGINFO_PATH);

        settOppRestApi(servletContextEvent, apiApplication, konfigurator);
        if (soapTjenesterEksisterer(servletContext)) {
            leggTilServlet(servletContextEvent, new SoapServlet(), WEBSERVICE_PATH);
        }

        if (sensuMetricsBrukes()) {
            MetricsClient.enableMetrics(MetricsConfig.resolveNaisConfig());
        } else {
            LOGGER.info("Sensu metrics deaktivert");
        }

        webApplicationContext.getAutowireCapableBeanFactory().autowireBean(apiApplication);

        LOGGER.info("contextInitialized - slutt");
        apiApplication.startup(servletContext);
    }

    private Collection<? extends Pingable> resolvePingables(WebApplicationContext webApplicationContext, Konfigurator konfigurator) {
        List<Pingable> pingables = new ArrayList<>();
        pingables.addAll(webApplicationContext.getBeansOfType(Pingable.class).values());
        pingables.addAll(konfigurator.getPingables());
        return pingables;
    }

    private boolean sensuMetricsBrukes() {
        boolean sensuDisabled = getOptionalProperty(ApiApp.DISABLE_SENSU_METRICS).map(Boolean::parseBoolean).orElse(false);
        return !sensuDisabled;
    }

    private boolean stsBrukes() {
        return getOptionalProperty(StsSecurityConstants.STS_URL_KEY).isPresent();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().invalidate();
        throw new IllegalStateException("api-apps should be stateless");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {}

    private void konfigurerSpring(ServletContext servletContext) {
        servletContext.setInitParameter(CONTEXT_CLASS_PARAM, AnnotationConfigWebApplicationContext.class.getName());
        servletContext.setInitParameter(CONFIG_LOCATION_PARAM, "");
        contextLoaderListener.setContextInitializers(applicationContext -> {
            AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = (AnnotationConfigWebApplicationContext) applicationContext;
            annotationConfigWebApplicationContext.register(apiApplication.getClass());
        });
    }

    private AnnotationConfigWebApplicationContext startSpring(ServletContextEvent servletContextEvent) {
        contextLoaderListener.contextInitialized(servletContextEvent);
        AnnotationConfigWebApplicationContext webApplicationContext = getSpringContext(servletContextEvent);
        webApplicationContext.getBean(ApiApplication.class);

        leggTilBonne(servletContextEvent, new LedigDiskPlassHelsesjekk());
        leggTilBonne(servletContextEvent, new TruststoreHelsesjekk());
        if (stsBrukes()) {
            leggTilBonne(servletContextEvent, new STSHelsesjekk());
        }
        return webApplicationContext;
    }

    private void leggTilBonne(ServletContextEvent servletContextEvent, Object bonne) {
        getSpringContext(servletContextEvent).getBeanFactory().registerSingleton(bonne.getClass().getName(), bonne);
    }

    private void settOppRestApi(ServletContextEvent servletContextEvent, ApiApplication apiApplication, Konfigurator konfigurator) {
        WebApplicationContext webApplicationContext = getContext(servletContextEvent.getServletContext());
        RestApplication restApplication = new RestApplication(webApplicationContext, apiApplication, konfigurator);
        ServletContainer servlet = new ServletContainer(ResourceConfig.forApplication(restApplication));
        ServletRegistration.Dynamic servletRegistration = leggTilServlet(servletContextEvent, servlet, sluttMedSlash(apiApplication.getApiBasePath()) + "*");
        SwaggerResource.setupServlet(servletRegistration);
    }


}