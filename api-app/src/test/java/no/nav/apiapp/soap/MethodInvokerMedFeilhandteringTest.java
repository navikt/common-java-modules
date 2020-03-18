package no.nav.apiapp.soap;

import no.nav.apiapp.TestContext;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import org.junit.Test;

import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import static no.nav.apiapp.feil.FeilMapper.VIS_DETALJER_VED_FEIL;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.assertj.core.api.Assertions.assertThat;

public class MethodInvokerMedFeilhandteringTest {

    static {
        TestContext.setup();
    }

    private MethodInvokerMedFeilhandtering methodInvokerMedFeilhandtering = new MethodInvokerMedFeilhandtering(new Object());

    @Test
    public void findSoapFaultException() {
        setTemporaryProperty(VIS_DETALJER_VED_FEIL, Boolean.FALSE.toString(), () -> {
            SOAPFaultException soapFaultException = methodInvokerMedFeilhandtering.findSoapFaultException(new RuntimeException());
            SOAPFault soapFault = soapFaultException.getFault();
            assertThat(soapFault.getFaultString()).matches("^[a-z0-9]{32}$");
            assertThat(soapFault.getFaultCode()).isEqualTo(FeilType.UKJENT.name());
            assertThat(soapFault.hasDetail()).isFalse();
        });
    }

    @Test
    public void findSoapFaultException__med_detaljer() {
        setTemporaryProperty(VIS_DETALJER_VED_FEIL, Boolean.TRUE.toString(), () -> {
            SOAPFaultException soapFaultException = methodInvokerMedFeilhandtering.findSoapFaultException(new RuntimeException());
            assertThat(soapFaultException.getFault().hasDetail()).isTrue();
        });
    }

}