package no.nav.apiapp.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.jaxrs.config.DefaultJaxrsScanner;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import org.springframework.stereotype.Component;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.ApiAppServletContextListener.API_PATH;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

@Component
@Path("/" + SwaggerResource.SWAGGER_JSON)
public class SwaggerResource extends BaseApiListingResource {

    public static final String SWAGGER_JSON = "swagger.json";

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
        new DefaultJaxrsScanner().classesFromContext(app, sc).forEach(res -> leggTilStandardDokumentasjon(swagger, res));
        return swagger;
    }

    private void leggTilStandardDokumentasjon(Swagger swagger, Class<?> contextClass) {
        ofNullable(findAnnotation(contextClass, Path.class))
                .map(Path::value)
                .map(this::prependSlash)
                .ifPresent(resourcePath -> leggTilStandardDokumentasjon(swagger, contextClass, contextClass, resourcePath));
    }

    private void leggTilStandardDokumentasjon(Swagger swagger, Class<?> methodsClass, Class<?> contextClass, String resourcePath) {
        if (!skalIkkeHaEkstraDokumentasjon(methodsClass)) {
            Arrays.stream(methodsClass.getMethods())
                    .filter(method -> method.getDeclaringClass() != Object.class)
                    .forEach(method -> leggTilStandardDokumentasjon(swagger, method, resourcePath, contextClass));
        }
    }

    private boolean skalIkkeHaEkstraDokumentasjon(Class<?> methodsClass) {
        return methodsClass == Object.class
                || methodsClass.getName().startsWith("java") // java.lang, java.util, javax.*
                || methodsClass.isEnum()
                ;
    }

    private void leggTilStandardDokumentasjon(Swagger swagger, Method method, String resourcePath, Class<?> contextClass) {
        String methodPath = ofNullable(findAnnotation(method,Path.class))
                .map(Path::value)
                .map(this::prependSlash)
                .orElse("");
        String path = resourcePath + ("/".equals(methodPath) ? "" : methodPath);
        leggTilStandardDokumentasjon(swagger, method.getReturnType(), contextClass, path);
        ofNullable(swagger.getPath(path))
                .flatMap(swaggerPath -> getOperation(swaggerPath, method))
                .ifPresent(operation -> {
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

}
