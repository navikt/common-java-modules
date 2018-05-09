package no.nav.fo.apiapp.soap;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.apiapp.JettyTest;
//import no.nav.modig.security.ws.SystemSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import static no.nav.fo.apiapp.soap.SoapEksempel.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SoapTest extends JettyTest {

    private static Aktoer_v2PortType aktoer_v2PortType;

    @BeforeClass
    public static void setup() {
        aktoer_v2PortType = new CXFClient<>(Aktoer_v2PortType.class)
                .address(uri("/ws" + TJENESTENAVN).toString())
                .configureStsForSystemUser()
                .withOutInterceptor(new LoggingOutInterceptor())
                .build();
    }

    @Test
    public void kanKommunisereMedTjeneste() throws HentAktoerIdForIdentPersonIkkeFunnet {
        assertThat(hentAktorId("1234"), equalTo("aktor-1234"));
    }

    @Test
    public void tjenestePropagererSoapFeil() throws HentAktoerIdForIdentPersonIkkeFunnet {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_NOSTET_KALL, Feil.Type.UKJENT, SOAPFaultException.class);
        sjekkAtTjenesteFeilerMed(IDENT_FOR_NOSTET_KALL, Feil.Type.UKJENT, "<SOAP-ENV:Fault");
    }

    @Test
    public void tjenestePropagererUkjenteFeil() throws HentAktoerIdForIdentPersonIkkeFunnet {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_UKJENT_FEIL, Feil.Type.UKJENT, Throwable.class);
    }

    @Test
    public void tjenestePropagererKjenteFeil() throws HentAktoerIdForIdentPersonIkkeFunnet {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_VERSJONSKONFLIKT, Feil.Type.VERSJONSKONFLIKT, VersjonsKonflikt.class);
    }

    private String hentAktorId(String ident) throws HentAktoerIdForIdentPersonIkkeFunnet {
        WSHentAktoerIdForIdentRequest hentAktoerIdForIdentRequest = new WSHentAktoerIdForIdentRequest();
        hentAktoerIdForIdentRequest.setIdent(ident);
        WSHentAktoerIdForIdentResponse wsHentAktoerIdForIdentResponse = aktoer_v2PortType.hentAktoerIdForIdent(hentAktoerIdForIdentRequest);
        return wsHentAktoerIdForIdentResponse.getAktoerId();
    }

    private void sjekkAtTjenesteFeilerMed(String parameter, Feil.Type feilType, Class<? extends Throwable> internFeil) throws HentAktoerIdForIdentPersonIkkeFunnet {
        sjekkAtTjenesteFeilerMed(parameter, feilType, internFeil.getName());
    }

    private void sjekkAtTjenesteFeilerMed(String parameter, Feil.Type feilType, String feilMelding) throws HentAktoerIdForIdentPersonIkkeFunnet {
        try {
            hentAktorId(parameter);
            fail();
        } catch (SOAPFaultException s) {
            SOAPFault fault = s.getFault();
            assertThat(fault.getFaultCodeAsQName().getLocalPart(), equalTo(feilType.name()));
            assertThat(fault.getDetail().getTextContent(), containsString(feilMelding));
        }
    }

}
