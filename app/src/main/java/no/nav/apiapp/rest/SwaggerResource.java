package no.nav.apiapp.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.*;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.ApiAppServletContextListener.API_PATH;

@Component
@Path("/" + SwaggerResource.SWAGGER_JSON)
public class SwaggerResource extends BaseApiListingResource {

    public static final String SWAGGER_JSON = "swagger.json";

    private final ApplicationContext applicationContext;

    public SwaggerResource(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void setupServlet(ServletRegistration.Dynamic servletRegistration) {
        servletRegistration.setInitParameter("scan.all.resources", "true");
    }

    @GET
    public Response getSwaggerJSON(
            @Context Application application,
            @Context ServletConfig servletConfig,
            @Context ServletContext servletContext,
            @Context HttpHeaders httpHeaders,
            @Context UriInfo uriInfo
    ) throws JsonProcessingException {
        ReaderConfigUtils.initReaderConfig(servletConfig);
        return getListingJsonResponse(application, servletContext, servletConfig, httpHeaders, uriInfo);
    }

    @Override
    protected Swagger process(Application app, ServletContext servletContext, ServletConfig sc, HttpHeaders headers, UriInfo uriInfo) {
        Swagger swagger = super.process(app, servletContext, sc, headers, uriInfo);
        swagger.setBasePath(servletContext.getContextPath() + API_PATH);
        applicationContext.getBeansWithAnnotation(Path.class).values().forEach(res -> leggTilStandardDokumentasjon(swagger, res));
        return swagger;
    }

    private void leggTilStandardDokumentasjon(Swagger swagger, Object resource) {
        Arrays.stream(resource.getClass().getDeclaredMethods()).forEach(method -> leggTilStandardDokumentasjon(swagger, method));
    }

    private void leggTilStandardDokumentasjon(Swagger swagger, Method method) {
        String methodPath = ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("");
        String path = getResourcePath(method) + methodPath;
        ofNullable(swagger.getPath(path)).flatMap(swaggerPath -> getOperation(swaggerPath, method)).ifPresent(operation -> {
            if (ofNullable(operation.getTags()).map(List::isEmpty).orElse(true)) {
                operation.addTag(method.getDeclaringClass().getSimpleName());
            }
            operation.setSummary(ofNullable(operation.getSummary())
                    .orElse(method.getName())
            );
        });
    }

    private Optional<Operation> getOperation(io.swagger.models.Path swaggerPath, Method method) {
        Map<io.swagger.models.HttpMethod, Operation> operationsByMethod = swaggerPath.getOperationMap();
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(annotationType -> annotationType.isAnnotationPresent(HttpMethod.class))
                .map(annotationType -> io.swagger.models.HttpMethod.valueOf(annotationType.getSimpleName()))
                .map(operationsByMethod::get)
                .findAny();
    }

    private String getResourcePath(Method method) {
        return method.getDeclaringClass().getAnnotation(Path.class).value();
    }

}
