package no.nav.sbl.dialogarena.common.cxf;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static no.nav.modig.core.context.SubjectHandler.getSubjectHandler;
import static org.slf4j.LoggerFactory.getLogger;

/**
 For bruk i applikasjoner som eksponerer soap-tjenester.
 Denne brukes til å konfigurere en soap-klient til å propagere SAML-tokenet
 den mottok videre til neste soap-tjeneste.
 */

public class SamlPropagatingOutInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = getLogger(SamlPropagatingOutInterceptor.class);

    public SamlPropagatingOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        Element samlAssertion = getSubjectHandler().getSAMLAssertion();
        QName qName = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder;
            Document document;
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(
                    new StringReader("<wsse:Security " +
                            "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" " +
                            "xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">" +
                            "</wsse:Security>")));
            Node node = document.importNode(samlAssertion, true);
            Element documentElement = document.getDocumentElement();
            documentElement.appendChild(node);
            SoapHeader header = new SoapHeader(qName, documentElement);
            header.setMustUnderstand(true);
            message.getHeaders().add(header);
        } catch (Exception e) {
            LOG.error("exception", e);
        }
    }
}
