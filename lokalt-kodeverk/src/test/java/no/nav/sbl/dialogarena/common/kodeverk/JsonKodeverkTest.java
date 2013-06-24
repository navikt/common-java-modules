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
import static org.junit.Assert.assertThat;

public class JsonKodeverkTest {

    private Kodeverk kodeverk;

    @Before
    public void setup() {
        kodeverk = new JsonKodeverk(getClass().getResourceAsStream("/kodeverk_test.json"));
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
    public void canGetExistingKodeverkByField() {
        assertThat(kodeverk.getKode("test", Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(kodeverk.getKode("test", Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(kodeverk.getKode("test", Nokkel.TEMA), is(equalTo("tema")));
        assertThat(kodeverk.getKode("test", Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(kodeverk.getKode("test", Nokkel.URL), is(equalTo("url")));
        assertThat(kodeverk.getKode("test", Nokkel.VEDLEGGSID), is(equalTo("vedleggsid")));
        assertThat(kodeverk.getKode("test", Nokkel.URLENGLISH), is(equalTo("urlEnglish")));
    }

    @Test
    public void canGetExistingKodeverkByMap() {
        Map<Nokkel, String> koder = kodeverk.getKoder("test");
        assertThat(koder.get(Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(koder.get(Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(koder.get(Nokkel.TEMA), is(equalTo("tema")));
        assertThat(koder.get(Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(koder.get(Nokkel.URL), is(equalTo("url")));
        assertThat(koder.get(Nokkel.VEDLEGGSID), is(equalTo("vedleggsid")));
        assertThat(kodeverk.getKode("test", Nokkel.URLENGLISH), is(equalTo("urlEnglish")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void canNotAlterKodeverk() {
        Map<Kodeverk.Nokkel, String> koder = kodeverk.getKoder("test");
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
    public void shouldRecognizeEgendefKode(){
        assertThat(kodeverk.isEgendefinert(Kodeverk.ANNET),is(true));
        assertThat(kodeverk.isEgendefinert("hei"),is(false));
    }

    @Test
    public void shouldGetTittel() {
        assertThat(kodeverk.getTittel("test"), equalTo("tittel"));
    }

}
