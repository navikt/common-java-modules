package no.nav.apiapp.soap;

import lombok.SneakyThrows;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.StringWriter;

import static javax.xml.transform.OutputKeys.INDENT;

public class SoapFeilMapper {

    @SneakyThrows
    public static String finnStackTrace(SOAPFaultException exception) {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(INDENT,"yes");
        transformer.transform(new DOMSource(exception.getFault()), new StreamResult(writer));
        return writer.toString();
    }

}

