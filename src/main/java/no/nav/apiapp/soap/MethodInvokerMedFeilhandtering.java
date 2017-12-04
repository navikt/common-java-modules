package no.nav.apiapp.soap;

import no.nav.apiapp.feil.FeilDTO;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import static no.nav.apiapp.feil.FeilMapper.somFeilDTO;
import static no.nav.apiapp.util.EnumUtils.getName;
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
        LOGGER.error(throwable.getMessage(), throwable);
        FeilDTO feilDTO = somFeilDTO(throwable);
        try {
            SOAPFault fault = soapFactory.createFault();
            fault.setFaultString(feilDTO.id);
            fault.setFaultCode(getName(feilDTO.type));
            fault.addDetail().addTextNode(toJson(feilDTO.detaljer));
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            LOGGER.error(e.getMessage(), e);
            return super.findSoapFaultException(throwable);
        }
    }

}
