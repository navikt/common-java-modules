package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.TimeZone;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.json.TestUtils.assertEqualJson;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

public class DatoTest extends JettyTest {

    private static TimeZone currentTimeZone ;

    @BeforeClass
    public static void setup() {
        currentTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
    }

    @AfterClass
    public static void teardown() {
        TimeZone.setDefault(currentTimeZone);
    }

    // NB: backenden sender 2017-05-10T01:02:03.000000004+02:00[Europe/Paris]
    private static final String DTO_JSON = "{" +
            "\"localDate\":\"2017-05-10T12:00:00+02:00\"," +
            "\"localDateTime\":\"2017-05-10T01:02:03.000000004+02:00\"," +
            "\"zonedDateTime\":\"2017-05-10T01:02:03.000000004+02:00\"," +
            "\"date\":\"2017-05-10T01:02:03+02:00\"," +
            "\"aBoolean\":true," +
            "\"optionalDate\":\"2017-05-10T01:02:03+02:00\"," +
            "\"noOptionalDate\":null," +
            "\"string\":null," +
            "\"timestamp\":\"2017-05-10T01:02:03+02:00\"," +
            "\"sqlDate\":\"2017-05-10T00:00:00+02:00\"" +
            "}";

    private static final String QUERY_TEST_STRING = "dette er en test";

    private static final String QUERY_UTC_DATO = "2017-05-12T13:47:30Z";
    private static final String QUERY_UTC = "{" +
            "\"localDate\":\"2017-05-12T13:47:30Z\"," +
            "\"localDateTime\":\"2017-05-12T13:47:30Z\"," +
            "\"zonedDateTime\":\"2017-05-12T13:47:30Z\"," +
            "\"date\":\"2017-05-12T13:47:30Z\"," +
            "\"string\":\"dette er en test\"," +
            "\"timestamp\":\"2017-05-12T13:47:30Z\"," +
            "\"sqlDate\":\"2017-05-12T13:47:30Z\"" +
            "}";

    private static final String DTO_FRA_UTC_QUERY = "{" +
            "\"localDate\":\"2017-05-12T12:00:00+02:00\"," +
            "\"localDateTime\":\"2017-05-12T15:47:30+02:00\"," +
            "\"zonedDateTime\":\"2017-05-12T13:47:30Z\"," +
            "\"date\":\"2017-05-12T15:47:30+02:00\"," +
            "\"aBoolean\":true," +
            "\"optionalDate\":\"2017-05-12T15:47:30+02:00\"," +
            "\"noOptionalDate\":null," +
            "\"string\":\"dette er en test\"," +
            "\"timestamp\":\"2017-05-12T15:47:30+02:00\"," +
            "\"sqlDate\":\"2017-05-12T00:00:00+02:00\"" +
            "}";

    private static final String QUERY_PARIS_DATO = "2017-05-12T15:47:30+02:00";
    private static final String QUERY_PARIS = "{" +
            "\"localDate\":\"2017-05-12T15:47:30+02:00\"," +
            "\"localDateTime\":\"2017-05-12T15:47:30+02:00\"," +
            "\"zonedDateTime\":\"2017-05-12T15:47:30+02:00\"," +
            "\"date\":\"2017-05-12T15:47:30+02:00\"," +
            "\"string\":\"dette er en test\"," +
            "\"timestamp\":\"2017-05-12T15:47:30+02:00\"," +
            "\"sqlDate\":\"2017-05-12T15:47:30+02:00\"" +
            "}";
    private static final String DTO_FRA_PARIS_QUERY = "{" +
            "\"localDate\":\"2017-05-12T12:00:00+02:00\"," +
            "\"localDateTime\":\"2017-05-12T15:47:30+02:00\"," +
            "\"zonedDateTime\":\"2017-05-12T15:47:30+02:00\"," +
            "\"date\":\"2017-05-12T15:47:30+02:00\"," +
            "\"aBoolean\":true," +
            "\"optionalDate\":\"2017-05-12T15:47:30+02:00\"," +
            "\"noOptionalDate\":null," +
            "\"string\":\"dette er en test\"," +
            "\"timestamp\":\"2017-05-12T15:47:30+02:00\"," +
            "\"sqlDate\":\"2017-05-12T00:00:00+02:00\"" +
            "}";

    private static final String NULL_JSON = "{" +
            "\"localDate\":null," +
            "\"localDateTime\":null," +
            "\"zonedDateTime\":null," +
            "\"date\":null," +
            "\"aBoolean\":true," +
            "\"optionalDate\":null," +
            "\"noOptionalDate\":null," +
            "\"string\":" +
            "\"dette er en test\"," +
            "\"timestamp\":null," +
            "\"sqlDate\":null" +
            "}";

    @Test
    public void getDTO() {
        assertEqualJson(getString("/api/dato/dto"), DTO_JSON);
    }

    @Test
    public void queryUTC() {
        testQuery(QUERY_UTC_DATO, DTO_FRA_UTC_QUERY);
    }

    @Test
    public void queryParis() {
        testQuery(QUERY_PARIS_DATO, DTO_FRA_PARIS_QUERY);
    }

    @Test
    public void queryNull() {
        testQuery(null, NULL_JSON);
    }

    @Test
    public void queryPostUTC() throws Exception {
        testPost(QUERY_UTC, DTO_FRA_UTC_QUERY);
    }

    @Test
    public void queryPostParis() throws Exception {
        testPost(QUERY_PARIS, DTO_FRA_PARIS_QUERY);
    }

    @Test
    public void queryPostNull() throws Exception {
        testPost(null, DTO_JSON);
    }

    private void testPost(String queryParis, String dtoFraParisQuery) {
        String response = target("/api/dato/query")
                .request()
                .header(PREFERRED_NAV_CALL_ID_HEADER_NAME, "callId")
                .header(CONSUMER_ID_HEADER_NAME, "consumerId")
                .post(Entity.entity(queryParis, APPLICATION_JSON_TYPE))
                .readEntity(String.class);
        assertEqualJson(response, dtoFraParisQuery);
    }

    private void testQuery(String queryDato, String forventetRespons) {
        String response = target("/api/dato/query")
                .queryParam("date", queryDato)
                .queryParam("zonedDateTime", queryDato)
                .queryParam("localDateTime", queryDato)
                .queryParam("localDate", queryDato)
                .queryParam("string", QUERY_TEST_STRING)
                .queryParam("timestamp", queryDato)
                .queryParam("sqlDate", queryDato)
                .request()
                .header(PREFERRED_NAV_CALL_ID_HEADER_NAME, "callId")
                .header(CONSUMER_ID_HEADER_NAME, "consumerId")
                .get()
                .readEntity(String.class);
        assertEqualJson(forventetRespons, response);
    }

}