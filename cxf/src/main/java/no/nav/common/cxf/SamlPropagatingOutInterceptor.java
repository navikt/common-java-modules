package no.nav.common.cxf;

import lombok.SneakyThrows;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.common.cxf.saml.SamlUtils;
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

import static org.slf4j.LoggerFactory.getLogger;

/**
 * For bruk i applikasjoner som eksponerer soap-tjenester.
 * Denne brukes til å konfigurere en soap-klient til å propagere SAML-tokenet
 * den mottok videre til neste soap-tjeneste.
 */

public class SamlPropagatingOutInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = getLogger(SamlPropagatingOutInterceptor.class);

    public SamlPropagatingOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    @SneakyThrows
    public void handleMessage(SoapMessage message) throws Fault {
        Element samlAssertion = SubjectHandler.getSsoToken(SsoToken.Type.SAML)
                .map(SamlUtils::toSamlAssertion)
                .orElseThrow(IllegalStateException::new)
                .getDOM();
        QName qName = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(
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
    }

}
