package no.nav.sbl.dialogarena.common.kodeverk;


import no.nav.modig.core.exception.ApplicationException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.common.kodeverk.Kodeverk.Nokkel;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class KodeverkTest {

    private Kodeverk kodeverk;

    @Before
    public void setup() {
        kodeverk = new DefaultKodeverk();
    }

    @Test
    public void canCreateKodeverk() {
        assertThat(kodeverk, is(notNullValue()));
    }

    @Test
    public void canGetExistingKodeverkByField() {
        createTestData();
        assertThat(kodeverk.getKode("test", Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(kodeverk.getKode("test", Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(kodeverk.getKode("test", Nokkel.TEMA), is(equalTo("tema")));
        assertThat(kodeverk.getKode("test", Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(kodeverk.getKode("test", Nokkel.URL), is(equalTo("url")));
    }

    @Test
    public void canGetExistingKodeverkByMap() {
        createTestData();
        Map<Kodeverk.Nokkel, String> koder = kodeverk.getKoder("test");
        assertThat(koder.get(Nokkel.BESKRIVELSE), is(equalTo("beskrivelse")));
        assertThat(koder.get(Nokkel.GOSYS_ID), is(equalTo("gosysId")));
        assertThat(koder.get(Nokkel.TEMA), is(equalTo("tema")));
        assertThat(koder.get(Nokkel.TITTEL), is(equalTo("tittel")));
        assertThat(koder.get(Nokkel.URL), is(equalTo("url")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void canNotAlterKodeverk() {
        createTestData();
        Map<Kodeverk.Nokkel, String> koder = kodeverk.getKoder("test");
        koder.put(Nokkel.BESKRIVELSE, "feilbeskrivelse");
    }

    @Test(expected = ApplicationException.class)
    public void unknownKodeverkMapThrowsException(){
        kodeverk.getKoder("test");
    }

    @Test(expected = ApplicationException.class)
    public void unknownKodeverkThrowsException(){
        kodeverk.getKode("test",Nokkel.URL);
    }

    private void createTestData() {
        Map<String, KodeverkElement> testData = new HashMap<>();
        testData.put("test", new KodeverkElement("test", "gosysId", "tema", "beskrivelse", "tittel", "url"));
        setInternalState(kodeverk, "db", testData);
    }


}
