package no.nav.fo.apiapp.rest;

import no.nav.apiapp.rest.ExceptionMapper;
import no.nav.fo.apiapp.JettyTest;
import no.nav.sbl.dialogarena.types.feil.FeilType;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.Response.Status.*;
import static no.nav.sbl.dialogarena.types.feil.FeilType.*;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.json.TestUtils.assertEqualJson;
import static org.assertj.core.api.Assertions.assertThat;

public class RestTest extends JettyTest {

    @Test
    public void get() {
        assertThat(getString("/api/eksempel")).isEqualToIgnoringCase("eksempel");
    }

    @Test
    public void noCache() {
        Response response = get("/api/eksempel");
        sjekkStatus(response,OK);
        assertThat(response.getHeaderString(CACHE_CONTROL)).isEqualToIgnoringCase("no-cache, no-store, must-revalidate");
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
        assertThat(response.getStatus()).isEqualTo(status.getStatusCode());
    }

    private void sjekkFeilInformasjon(Response response, FeilType type) {
        String json = response.readEntity(String.class);
        Map<String, Object> feilDTO = fromJson(json, Map.class);
        assertThat(feilDTO.get("id")).isNotNull();
        assertThat(feilDTO.get("type")).isEqualTo(type.name());
        assertThat(feilDTO.get("detaljer")).isNotNull();

        assertThat(response.getHeaderString(ExceptionMapper.ESCAPE_REDIRECT_HEADER)).isNotEmpty();
    }

}
