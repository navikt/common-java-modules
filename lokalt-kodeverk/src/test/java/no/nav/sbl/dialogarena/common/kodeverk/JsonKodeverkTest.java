package no.nav.sbl.dialogarena.common.kodeverk;

import no.nav.modig.core.exception.ApplicationException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JsonKodeverkTest {

    private Kodeverk kodeverk;
    private static final String TEST = "test";

    @Before
    public void setup() {
        kodeverk = new JsonKodeverk(getClass().getResourceAsStream("/kodeverk_test.json"));
    }

    @Test
    public void shouldThrowExceptionWhenParsingFails() {
        try {
            new JsonKodeverk(null);
        } catch (ApplicationException ae) {
            assertThat(ae.getMessage(), is("Klarte ikke å parse kodeverk-info"));
        }
    }

    @Test(expected = ApplicationException.class)
    public void shouldThrowExceptionWhenMissingValue() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/kodeverk_feil.json");
        new JsonKodeverk(resourceAsStream);
    }

    @Test
    public void shouldReadEmptyFile() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/kodeverk_tom.json");
        new JsonKodeverk(resourceAsStream);
    }

    @Test
    public void shouldGetVedleggById() {
        Map<Kodeverk.Nokkel, String> koder = kodeverk.getKoder("vedleggsid");
        assertThat(koder, notNullValue());
    }

    @Test
    public void canGetExistingKodeverkByField() {
        assertThat(kodeverk.getKode(TEST, Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(kodeverk.getKode(TEST, Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(kodeverk.getKode(TEST, Nokkel.TEMA), is(equalTo("tema")));
        assertThat(kodeverk.getKode(TEST, Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URL), is(equalTo("url")));
        assertThat(kodeverk.getKode(TEST, Nokkel.VEDLEGGSID), is(equalTo("vedleggsid")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLENGLISH), is(equalTo("urlEnglish")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLNEWNORWEGIAN), is(equalTo("urlNewnorwegian")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLPOLISH), is(equalTo("urlPolish")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLFRENCH), is(equalTo("urlFrench")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLSPANISH), is(equalTo("urlSpanish")));
        assertThat(kodeverk.getKode(TEST, Nokkel.URLGERMAN), is(equalTo("urlGerman")));
    }

    @Test
    public void canGetExistingKodeverkByMap() {
        Map<Nokkel, String> koder = kodeverk.getKoder(TEST);
        assertThat(koder.get(Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(koder.get(Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(koder.get(Nokkel.TEMA), is(equalTo("tema")));
        assertThat(koder.get(Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(koder.get(Nokkel.URL), is(equalTo("url")));
        assertThat(koder.get(Nokkel.VEDLEGGSID), is(equalTo("vedleggsid")));
        assertThat(koder.get(Nokkel.URLENGLISH), is(equalTo("urlEnglish")));
        assertThat(koder.get(Nokkel.URLNEWNORWEGIAN), is(equalTo("urlNewnorwegian")));
        assertThat(koder.get(Nokkel.URLPOLISH), is(equalTo("urlPolish")));
        assertThat(koder.get(Nokkel.URLFRENCH), is(equalTo("urlFrench")));
        assertThat(koder.get(Nokkel.URLSPANISH), is(equalTo("urlSpanish")));
        assertThat(koder.get(Nokkel.URLGERMAN), is(equalTo("urlGerman")));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void canNotAlterKodeverk() {
        Map<Kodeverk.Nokkel, String> koder = kodeverk.getKoder(TEST);
        koder.put(Nokkel.BESKRIVELSE, "feilbeskrivelse");
    }

    @Test(expected = ApplicationException.class)
    public void unknownKodeverkMapThrowsException() {
        kodeverk.getKoder("unknown");
    }

    @Test(expected = ApplicationException.class)
    public void unknownKodeverkThrowsException() {
        kodeverk.getKode("unknown", Nokkel.URL);
    }

    @Test
    public void shouldRecognizeEgendefKode() {
        assertThat(kodeverk.isEgendefinert(Kodeverk.ANNET), is(true));
        assertThat(kodeverk.isEgendefinert("hei"), is(false));
    }

    @Test
    public void shouldGetTittel() {
        assertThat(kodeverk.getTittel(TEST), equalTo("tittel"));
    }
}