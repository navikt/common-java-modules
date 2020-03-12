package no.nav.apiapp.soap;

import lombok.SneakyThrows;
import no.nav.metrics.MetodeTimer;
import no.nav.sbl.dialogarena.types.feil.FeilDTO;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.message.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import java.lang.reflect.Method;

import static no.nav.apiapp.feil.FeilMapper.somFeilDTO;
import static no.nav.apiapp.util.StringUtils.notNullOrEmpty;
import static no.nav.json.JsonUtils.toJson;

public class MethodInvokerMedFeilhandtering extends JAXWSMethodInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodInvokerMedFeilhandtering.class);

    private final SOAPFactory soapFactory;

    public MethodInvokerMedFeilhandtering(Object serviceBean) {
        super(serviceBean);
        try {
            soapFactory = SOAPFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected SOAPFaultException findSoapFaultException(Throwable throwable) {
        FeilDTO feilDTO = somFeilDTO(throwable);
        try {
            SOAPFault fault = soapFactory.createFault();
            fault.setFaultString(feilDTO.id);
            fault.setFaultCode(feilDTO.type);
            String detaljerJson = toJson(feilDTO.detaljer);
            if (notNullOrEmpty(detaljerJson)) {
                fault.addDetail().addTextNode(detaljerJson);
            }
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            LOGGER.error(e.getMessage(), e);
            return super.findSoapFaultException(throwable);
        }
    }

    @Override
    @SneakyThrows
    protected Object performInvocation(Exchange exchange, Object serviceObject, Method m, Object[] paramArray) throws Exception {
        return MetodeTimer.timeMetode(() -> super.performInvocation(exchange, serviceObject, m, paramArray), "ws." + m.getName());
    }

}
