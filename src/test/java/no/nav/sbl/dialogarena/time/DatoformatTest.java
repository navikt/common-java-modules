package no.nav.sbl.dialogarena.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static no.nav.modig.lang.collections.FactoryUtils.always;
import static no.nav.sbl.dialogarena.time.Datoformat.kort;
import static no.nav.sbl.dialogarena.time.Datoformat.kortMedTid;
import static no.nav.sbl.dialogarena.time.Datoformat.kortUtenLiteral;
import static no.nav.sbl.dialogarena.time.Datoformat.lang;
import static no.nav.sbl.dialogarena.time.Datoformat.langMedTid;
import static no.nav.sbl.dialogarena.time.Datoformat.medium;
import static no.nav.sbl.dialogarena.time.Datoformat.mediumMedTid;
import static no.nav.sbl.dialogarena.time.Datoformat.ultrakort;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class DatoformatTest {


    private static final DateTime IDAG_KL_0805 = new DateTime(2013, 10, 7, 8, 5);
    private static final DateTime IGAAR_KL_0805 = IDAG_KL_0805.minusDays(1);

    @Before
    public void idagEr7Oktober2013INorge() {
        DateTimeUtils.setCurrentMillisFixed(IDAG_KL_0805.getMillis());
        Datoformat.brukLocaleFra(always(new Locale("nb", "no")));
    }

    @Test
    public void langDato() {
        assertThat(lang(new DateTime(1981, 6, 4, 12, 15)), is("torsdag 4. juni 1981"));
        assertThat(langMedTid(new DateTime(1981, 6, 4, 12, 15)), is("torsdag 4. juni 1981, kl 12:15"));
    }

    @Test
    public void mediumDato() {
        assertThat(medium(new DateTime(1981, 6, 4, 12, 15)), is("4. jun 1981"));
        assertThat(mediumMedTid(new DateTime(1981, 6, 4, 12, 15)), is("4. jun 1981, kl 12:15"));
    }

    @Test
    public void kortDato() {
        assertThat(kort(new DateTime(1981, 6, 4, 12, 15)), is("04.06.1981"));
        assertThat(kortUtenLiteral(new DateTime(1981, 6, 4, 12, 15)), is("04.06.1981"));
        assertThat(kortMedTid(new DateTime(1981, 6, 4, 12, 15)), is("04.06.1981 kl 12:15"));
    }

    @Test
    public void ultrakortDato() {
        assertThat(ultrakort(new DateTime(1981, 6, 4, 12, 15)), is("04.06.81"));
    }


    @Test
    public void datoIDag() {
        assertThat(lang(IDAG_KL_0805), is("i dag 7. oktober 2013"));
        assertThat(medium(IDAG_KL_0805), is("i dag"));
        assertThat(kort(IDAG_KL_0805), is("i dag"));
        assertThat(kortUtenLiteral(IDAG_KL_0805), is("07.10.2013"));

        assertThat(langMedTid(IDAG_KL_0805), is("i dag 7. oktober 2013, kl 08:05"));
        assertThat(mediumMedTid(IDAG_KL_0805), is("i dag, kl 08:05"));
        assertThat(kortMedTid(IDAG_KL_0805), is("i dag kl 08:05"));
    }


    @Test
    public void datoIGaar() {
        assertThat(lang(IGAAR_KL_0805), is("i går 6. oktober 2013"));
        assertThat(medium(IGAAR_KL_0805), is("i går"));
        assertThat(kort(IGAAR_KL_0805), is("i går"));
        assertThat(kortUtenLiteral(IGAAR_KL_0805), is("06.10.2013"));

        assertThat(langMedTid(IGAAR_KL_0805), is("i går 6. oktober 2013, kl 08:05"));
        assertThat(mediumMedTid(IGAAR_KL_0805), is("i går, kl 08:05"));
        assertThat(kortMedTid(IGAAR_KL_0805), is("i går kl 08:05"));
    }


    @Test
    public void datoImorgen() {
        DateTime imorgen = IDAG_KL_0805.plusDays(1);
        assertThat(lang(imorgen), is("i morgen 8. oktober 2013"));
        assertThat(medium(imorgen), is("i morgen"));
        assertThat(kort(imorgen), is("i morgen"));
        assertThat(kortUtenLiteral(imorgen), is("08.10.2013"));

        assertThat(langMedTid(imorgen), is("i morgen 8. oktober 2013, kl 08:05"));
        assertThat(mediumMedTid(imorgen), is("i morgen, kl 08:05"));
        assertThat(kortMedTid(imorgen), is("i morgen kl 08:05"));
    }



    @After
    public void resetToSystemClock() {
        DateTimeUtils.setCurrentMillisSystem();
        Datoformat.settTilbakeTilDefaultLocale();
    }


}
