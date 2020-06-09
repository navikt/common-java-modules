package no.nav.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class TestUtils {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.defaultObjectMapper();

    @SneakyThrows
    public static void assertEqualJson(String forventetRespons, String response) {
        assertThat(OBJECT_MAPPER.readTree(forventetRespons)).isEqualTo(OBJECT_MAPPER.readTree(response));
    }

    public static void assertEqualJsonArray(String forventetRespons, String response) {
        assertEqualJson(forventetRespons, response);
    }

}
