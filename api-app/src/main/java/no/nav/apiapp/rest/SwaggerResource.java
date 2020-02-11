package no.nav.apiapp.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.jaxrs.config.DefaultJaxrsScanner;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import lombok.SneakyThrows;
import no.nav.apiapp.ApiApplication;
import no.nav.log.LogFilter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

@Component
@Path("/" + SwaggerResource.SWAGGER_JSON)
public class SwaggerResource extends BaseApiListingResource {

    public static final String SWAGGER_JSON = "swagger.json";
    public static final String IKKE_BERIK = "ikke_berik";

    private final ApiApplication apiApplication;
    private final boolean hasAuthentication;

    public static void setupServlet(ServletRegistration.Dynamic servletRegistration) {
        servletRegistration.setInitParameter("scan.all.resources", "true");
    }

    public SwaggerResource(ApiApplication apiApplication, boolean hasAuthentication) {
        this.apiApplication = apiApplication;
        this.hasAuthentication = hasAuthentication;
    }

    @Inject
    private Provider<HttpServletRequest> httpServletRequestProvider;

    @GET
    public Response getSwaggerJSON(
            @Context Application application,
            @Context ServletConfig servletConfig,
            @Context ServletContext servletContext,
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo,
            @QueryParam(IKKE_BERIK) String ikkeBerik
    ) throws JsonProcessingException {
        ReaderConfigUtils.initReaderConfig(servletConfig);
        return getListingJsonResponse(application, servletContext, servletConfig, httpHeaders, uriInfo);
    }

    @Override
    protected Swagger process(Application app, ServletContext servletContext, ServletConfig sc, HttpHeaders headers, UriInfo uriInfo) {
        Swagger swagger = super.process(app, servletContext, sc, headers, uriInfo);
        if (!httpServletRequestProvider.get().getParameterMap().containsKey(IKKE_BERIK)) {
            return berik(
                    kopier(swagger), //  BaseApiListingResource cacher swagger-objektet, kopierer derfor for å ikke mutere på dette
                    app,
                    servletContext,
                    sc
            );
        } else {
            return swagger;
        }
    }

    @SneakyThrows
    private Swagger kopier(Swagger swagger) {
        ObjectMapper mapper = Json.mapper();
        return mapper.readValue(mapper.writeValueAsString(swagger), Swagger.class);
    }

    private Swagger berik(Swagger swagger, Application app, ServletContext servletContext, ServletConfig sc) {
        swagger.setBasePath(servletContext.getContextPath() + sluttMedSlash(apiApplication.getApiBasePath()));
        SwaggerRequest swaggerRequest = new SwaggerRequest(swagger);
        new DefaultJaxrsScanner().classesFromContext(app, sc).forEach(res -> leggTilStandardDokumentasjon(swaggerRequest, res));
        return swagger;
    }

    private void leggTilStandardDokumentasjon(SwaggerRequest swaggerRequest, Class<?> contextClass) {
        ofNullable(findAnnotation(contextClass, Path.class))
                .map(Path::value)
                .map(this::prependSlash)
                .ifPresent(resourcePath -> leggTilStandardDokumentasjon(swaggerRequest, contextClass, contextClass, resourcePath));
    }

    private void leggTilStandardDokumentasjon(SwaggerRequest swaggerRequest, Class<?> methodsClass, Class<?> contextClass, String resourcePath) {
        stream(methodsClass.getMethods()).forEach(method -> leggTilStandardDokumentasjon(swaggerRequest, method, resourcePath, contextClass));
    }

    private void leggTilStandardDokumentasjon(SwaggerRequest swaggerRequest, Method method, String resourcePath, Class<?> contextClass) {
        if (swaggerRequest.methods.contains(method)) {
            return;
        } else {
            swaggerRequest.methods.add(method);
        }

        String methodPath = ofNullable(findAnnotation(method, Path.class))
                .map(Path::value)
                .map(this::prependSlash)
                .orElse("");
        String path = resourcePath + ("/".equals(methodPath) ? "" : methodPath);
        leggTilStandardDokumentasjon(swaggerRequest, method.getReturnType(), contextClass, path);

        ofNullable(swaggerRequest.swagger.getPath(path))
                .flatMap(swaggerPath -> getOperation(swaggerPath, method))
                .ifPresent(operation -> {

                    HeaderParameter consumerIdParameter = new HeaderParameter();
                    consumerIdParameter.setName(LogFilter.CONSUMER_ID_HEADER_NAME);
                    consumerIdParameter.setDescription("the consuming entity of the request, typically the name of an application");
                    consumerIdParameter.setType("string");
                    operation.addParameter(consumerIdParameter);

                    HeaderParameter callIdParameter = new HeaderParameter();
                    callIdParameter.setName(LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME);
                    callIdParameter.setDescription("a correlation id that is added to logs associated with the request");
                    callIdParameter.setType("string");
                    operation.addParameter(callIdParameter);

                    if (hasAuthentication) {
                        HeaderParameter authorizationParameters = new HeaderParameter();
                        authorizationParameters.setName(HttpHeaders.AUTHORIZATION);
                        authorizationParameters.setType("string");
                        operation.addParameter(authorizationParameters);
                    }

                    io.swagger.models.Response authorizationResponse = new io.swagger.models.Response();
                    authorizationResponse.setDescription("the request was not authenticated or authorized");
                    operation.response(401, authorizationResponse);

                    operation.getResponses().values().forEach(response->{
                        response.addHeader(LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME, new StringProperty());
                    });

                    if (ofNullable(operation.getTags()).map(List::isEmpty).orElse(true)) {
                        operation.addTag(contextClass.getSimpleName());
                    }
                    operation.setSummary(ofNullable(operation.getSummary())
                            .orElse(method.getName())
                    );
                });
    }

    private Optional<Operation> getOperation(io.swagger.models.Path swaggerPath, Method method) {
        Map<io.swagger.models.HttpMethod, Operation> operationsByMethod = swaggerPath.getOperationMap();
        return ofNullable(findAnnotation(method, HttpMethod.class))
                .map(HttpMethod::value)
                .map(io.swagger.models.HttpMethod::valueOf)
                .map(operationsByMethod::get);
    }

    private String prependSlash(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static class SwaggerRequest {
        private Set<Method> methods = new HashSet<>();
        private final Swagger swagger;

        private SwaggerRequest(Swagger swagger) {
            this.swagger = swagger;
        }
    }

}
