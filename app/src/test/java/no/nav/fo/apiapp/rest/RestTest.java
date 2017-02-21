package no.nav.fo.apiapp.rest;

import no.nav.apiapp.feil.Feil;
import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static no.nav.apiapp.feil.Feil.Type.UKJENT;
import static no.nav.apiapp.feil.Feil.Type.VERSJONSKONFLIKT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

public class RestTest extends JettyTest {

    @Test
    public void get() {
        assertThat(getString("/api/eksempel"), equalTo("eksempel"));
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
        Map<String, Object> feilDTO = response.readEntity(Map.class);
        assertThat(feilDTO.get("id"), notNullValue());
        assertThat(feilDTO.get("type"), equalTo(type.name()));
        assertThat(feilDTO.get("detaljer"), notNullValue());
    }

}
