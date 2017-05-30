package no.nav.fo.apiapp.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.SneakyThrows;
import no.nav.fo.apiapp.JettyTest;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerTest extends JettyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerTest.class);

    @Test
    public void getUI() {
        assertThat(getString("/internal/swagger")).contains("<title>Swagger UI</title>");
    }

    @Test
    public void getSwaggerJson() throws Exception {
        ObjectMapper swaggerObjectMapper = Json.mapper();
        Swagger swagger = swaggerObjectMapper.readValue(uri("/api/swagger.json").toURL(), Swagger.class);
        LOGGER.info("swagger.json:\n{}\n", swaggerObjectMapper.writeValueAsString(swagger));
        Swagger forventet = swaggerObjectMapper.readValue(SwaggerTest.class.getResourceAsStream("/SwaggerTest.json"), Swagger.class);
        LOGGER.info("forventet swagger.json:\n{}\n", swaggerObjectMapper.writeValueAsString(forventet));
        assertThat(swagger).isEqualTo(forventet);
    }


}
