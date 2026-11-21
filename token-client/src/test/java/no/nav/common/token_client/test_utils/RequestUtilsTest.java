package no.nav.common.token_client.test_utils;

import static no.nav.common.token_client.test_utils.RequestUtils.parseFormData;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestUtilsTest {

    @Test
    void skal_beholde_alle_verdier_for_duplikate_parametere() {
        // Arrange
        String identicalValues = "scope=first&scope=first";
        String differentValues = "scope=first&scope=second";
        String encodedKey = "scope=first&sc%6Fpe=second";

        // Act
        Map<String, List<String>> identicalData = parseFormData(identicalValues);
        Map<String, List<String>> differentData = parseFormData(differentValues);
        Map<String, List<String>> encodedData = parseFormData(encodedKey);

        // Assert
        assertEquals(Map.of("scope", List.of("first", "first")), identicalData);
        assertEquals(Map.of("scope", List.of("first", "second")), differentData);
        assertEquals(Map.of("scope", List.of("first", "second")), encodedData);
    }

    @Test
    void skal_dekode_formdata_uten_a_miste_tomme_verdier_eller_likhetstegn() {
        // Arrange
        String encodedValues = "subject_token=abc%3Ddef&empty=&scope=test+scope";
        String unencodedValue = "subject_token=abc=def";

        // Act
        Map<String, List<String>> encodedData = parseFormData(encodedValues);
        Map<String, List<String>> unencodedData = parseFormData(unencodedValue);

        // Assert
        assertEquals(
                Map.of("subject_token", List.of("abc=def"), "empty", List.of(""), "scope", List.of("test scope")),
                encodedData);
        assertEquals(Map.of("subject_token", List.of("abc=def")), unencodedData);
    }
}
