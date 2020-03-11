package no.nav.fo.apiapp.soap;

import no.nav.fo.apiapp.JettyTest;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.feil.FeilType;
import no.nav.sbl.dialogarena.types.feil.VersjonsKonflikt;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.servicemix.examples.cxf.HelloWorld;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import static no.nav.fo.apiapp.soap.SoapEksempel.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

// TODO: Fjern testen eller prøv å få de til å kjøre med mocks
@Ignore
public class SoapTest extends JettyTest {

    private static HelloWorld helloWorld;

    @BeforeClass
    public static void setup() {
        helloWorld = new CXFClient<>(HelloWorld.class)
                .address(uri("/ws" + TJENESTENAVN).toString())
                .configureStsForSystemUser()
                .withOutInterceptor(new LoggingOutInterceptor())
                .build();
    }

    @Test
    public void kanKommunisereMedTjeneste()  {
        assertThat(sayHi("1234"), equalTo("hello 1234"));
    }

    @Test
    public void tjenestePropagererSoapFeil()  {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_NOSTET_KALL, FeilType.UKJENT, SOAPFaultException.class);
        sjekkAtTjenesteFeilerMed(IDENT_FOR_NOSTET_KALL, FeilType.UKJENT, "<SOAP-ENV:Fault");
    }

    @Test
    public void tjenestePropagererUkjenteFeil()  {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_UKJENT_FEIL, FeilType.UKJENT, Throwable.class);
    }

    @Test
    public void tjenestePropagererKjenteFeil()  {
        sjekkAtTjenesteFeilerMed(IDENT_FOR_VERSJONSKONFLIKT, FeilType.VERSJONSKONFLIKT, VersjonsKonflikt.class);
    }

    private String sayHi(String message)  {
        return helloWorld.sayHi(message);
    }

    private void sjekkAtTjenesteFeilerMed(String parameter, FeilType feilType, Class<? extends Throwable> internFeil)  {
        sjekkAtTjenesteFeilerMed(parameter, feilType, internFeil.getName());
    }

    private void sjekkAtTjenesteFeilerMed(String parameter, FeilType feilType, String feilMelding)  {
        try {
            sayHi(parameter);
            fail();
        } catch (SOAPFaultException s) {
            SOAPFault fault = s.getFault();
            assertThat(fault.getFaultCodeAsQName().getLocalPart(), equalTo(feilType.name()));
            assertThat(fault.getDetail().getTextContent(), containsString(feilMelding));
        }
    }

}
