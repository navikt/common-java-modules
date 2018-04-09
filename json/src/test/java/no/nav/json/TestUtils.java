package no.nav.json;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class TestUtils {

    public static void assertEqualJson(String forventetRespons, String response) {
        String normalisertRespons = new JSONObject(response).toString();
        String normalisertForventetRespons = new JSONObject(forventetRespons).toString();
        assertThat(normalisertRespons).isEqualTo(normalisertForventetRespons);
    }

    public static void assertEqualJsonArray(String forventetRespons, String response) {
        String normalisertRespons = new JSONArray(response).toString();
        String normalisertForventetRespons = new JSONArray(forventetRespons).toString();
        assertThat(normalisertRespons).isEqualTo(normalisertForventetRespons);
    }

}
