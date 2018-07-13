package no.nav.fo.apiapp.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static no.nav.apiapp.rest.SwaggerResource.IKKE_BERIK;
import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerTest extends JettyTest {

    private ObjectMapper swaggerObjectMapper = Json.mapper();

    @Test
    public void getUI() {
        assertRedirect("/internal/swagger", "/api-app/internal/swagger/");
        assertRedirect("/internal/swagger/", "/api-app/internal/swagger/index.html");

        Response response = get("/internal/swagger/index.html");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(String.class)).contains("<title>Swagger UI</title>");
        assertThat(response.getHeaderString(ETAG)).isNotEmpty();
        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo("text/html;charset=utf-8");
    }

    @Test
    public void getSwaggerJson() throws Exception {
        sammenlign(get(uri("/api/swagger.json").toURL()), read("/SwaggerTest.json"));
    }

    @Test
    public void getSwaggerDefaultJson() throws Exception {
        sammenlign(get(buildUri("/api/swagger.json").queryParam(IKKE_BERIK).build().toURL()), read("/SwaggerTest.default.json"));
    }

    private void assertRedirect(String path, String expectedRedirectPath) {
        Response redirectResponse = get(path);
        assertThat(redirectResponse.getLocation().getPath()).isEqualTo(expectedRedirectPath);
    }

    private Swagger read(String name) throws IOException {
        return swaggerObjectMapper.readValue(SwaggerTest.class.getResourceAsStream(name), Swagger.class);
    }

    private void sammenlign(Swagger swagger, Swagger forventet) throws JsonProcessingException {
        assertThat(swagger)
            .describedAs("\n\nfaktisk swagger.json:\n%s\n\nforventet swagger.json:\n%s\n\n",
                swaggerObjectMapper.writeValueAsString(swagger),
                swaggerObjectMapper.writeValueAsString(forventet)
            )
            .isEqualTo(forventet);
    }

    private Swagger get(URL src) throws IOException {
        return swaggerObjectMapper.readValue(src, Swagger.class);
    }

}
