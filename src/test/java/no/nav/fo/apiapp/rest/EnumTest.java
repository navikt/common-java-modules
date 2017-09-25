package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import static no.nav.json.TestUtils.assertEqualJson;
import static no.nav.json.TestUtils.assertEqualJsonArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class EnumTest extends JettyTest {

    private static final String ENUMER_JSON = "[\"ABC\",\"DEF\",\"GHI\"]";
    private static final String JSON_PAYLOAD = "\"ABC\"";
    private static final String DTO_PAYLOAD = "{\"enEnum\":\"ABC\",\"enums\":[\"ABC\",\"DEF\",\"GHI\"]}";

    @Test
    public void getEnumer() {
        assertEqualJsonArray(getString("/api/enum"), ENUMER_JSON);
    }

    @Test
    public void getEnum() {
        assertThat(getString("/api/enum/0"), equalTo(JSON_PAYLOAD));
    }

    @Test
    public void getEnumDTO() {
        assertEqualJson(getString("/api/enum/dto/0"), DTO_PAYLOAD);
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
        assertEqualJson(postJson("/api/enum", DTO_PAYLOAD), DTO_PAYLOAD);
    }

}
