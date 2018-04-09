package no.nav.fo.apiapp.rest;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.rest.ExceptionMapper;
import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Map;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static no.nav.apiapp.feil.Feil.Type.*;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.json.TestUtils.assertEqualJson;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

public class RestTest extends JettyTest {

    @Test
    public void get() {
        assertThat(getString("/api/eksempel"), equalTo("eksempel"));
    }

    @Test
    public void notFoundFeil() {
        Response notFoundResponse = get("/api/asdf/asdf/asdf");
        sjekkStatus(notFoundResponse, NOT_FOUND);
        sjekkFeilInformasjon(notFoundResponse, FINNES_IKKE);
    }

    @Test
    public void pipe() {
        String jsonPayload = "{\"a\":\"123\"}";
        assertEqualJson(putJson("/api/eksempel/pipe", jsonPayload), jsonPayload);
    }

    @Test
    public void deserialiseringsFeil() {
        Response response = put("/api/eksempel/pipe", entity("dette er ikke gyldig json!", MediaType.APPLICATION_JSON_TYPE));
        sjekkStatus(response, BAD_REQUEST);
        sjekkFeilInformasjon(response, UGYLDIG_REQUEST);
    }

    @Test
    public void konflikt() {
        Response response = get("/api/eksempel/konflikt");
        sjekkStatus(response, BAD_REQUEST);
        sjekkFeilInformasjon(response, VERSJONSKONFLIKT);
    }

    @Test
    public void ukjentFeil() {
        Response response = get("/api/eksempel/ukjent-feil");
        sjekkStatus(response, INTERNAL_SERVER_ERROR);
        sjekkFeilInformasjon(response, UKJENT);
    }

    private void sjekkStatus(Response response, Response.Status status) {
        assertThat(response.getStatus(), equalTo(status.getStatusCode()));
    }

    private void sjekkFeilInformasjon(Response response, Feil.Type type) {
        String json = response.readEntity(String.class);
        Map<String, Object> feilDTO = fromJson(json, Map.class);
        assertThat(feilDTO.get("id"), notNullValue());
        assertThat(feilDTO.get("type"), equalTo(type.name()));
        assertThat(feilDTO.get("detaljer"), notNullValue());

        assertThat(response.getHeaderString(ExceptionMapper.ESCAPE_REDIRECT_HEADER), not(isEmptyOrNullString()));
    }

}
