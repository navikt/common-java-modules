package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class EnumTest extends JettyTest {

    private static final String ENUMER_JSON = "[\"ABC\",\"DEF\",\"GHI\"]";
    private static final String JSON_PAYLOAD = "\"ABC\"";
    private static final String DTO_PAYLOAD = "{\"enEnum\":\"ABC\",\"enums\":[\"ABC\",\"DEF\",\"GHI\"]}";

    @Test
    public void getEnumer() {
        assertThat(getString("/api/enum"), equalTo(ENUMER_JSON));
    }

    @Test
    public void getEnum() {
        assertThat(getString("/api/enum/0"), equalTo(JSON_PAYLOAD));
    }

    @Test
    public void getEnumDTO() {
        assertThat(getString("/api/enum/dto/0"), equalTo(DTO_PAYLOAD));
    }

    @Test
    public void pipeEnum() {
        assertThat(putJson("/api/enum", JSON_PAYLOAD), equalTo(JSON_PAYLOAD));
        assertThat(putJson("/api/enum", ""), equalTo(""));
        String actual = putJson("/api/enum", "\"\"");
        assertThat(actual, equalTo(""));
    }

    @Test
    public void pipeDTO() {
        assertThat(postJson("/api/enum", DTO_PAYLOAD), equalTo(DTO_PAYLOAD));
    }

}
