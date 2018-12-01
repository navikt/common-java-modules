package no.nav.sbl.dialogarena.common.cxf.saml;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamlUtils {

    public static final String IDENT_TYPE = "identType";
    public static final String AUTHENTICATION_LEVEL = "authenticationLevel";
    public static final String CONSUMER_ID = "consumerId";

    private static final Logger logger = LoggerFactory.getLogger(SamlUtils.class);

    public static Subject samlAssertionToSubject(Assertion assertion) {
        String uid = assertion.getSubject().getNameID().getValue();
        uid = filterDNtoCNvalue(uid);
        String identType = null;
        List<Attribute> attributes = assertion.getAttributeStatements().get(0).getAttributes();

        Map<String, String> attributeMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            String attributeName = attribute.getName();
            String attributeValue = attribute.getAttributeValues().get(0)
                    .getDOM().getFirstChild().getTextContent();

            attributeMap.put(attributeName, attributeValue);

            if (IDENT_TYPE.equalsIgnoreCase(attributeName)) {
                identType = attributeValue;
            } else {
                logger.debug("Skipping SAML Attribute name: " + attribute.getName() + " value: " + attributeValue);
            }
        }
        if (uid == null) {
            throw new RuntimeException("SAML assertion is missing mandatory element NameId");
        }
        return new Subject(uid, IdentType.valueOf(identType), SsoToken.saml(getSamlAssertionAsString(assertion), attributeMap));
    }

    @SneakyThrows
    static String getSamlAssertionAsString(Assertion assertion) {
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(new DOMSource(assertion.getDOM()), new StreamResult(writer));
        return writer.toString();
    }

    @SneakyThrows
    public static Assertion toSamlAssertion(String assertion) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(assertion.getBytes("utf-8")));

        SamlAssertionWrapper assertionWrapper = new SamlAssertionWrapper(document.getDocumentElement());
        return assertionWrapper.getSaml2();
    }

    public static String filterDNtoCNvalue(String userid) {
        try {
            LdapName ldapname = new LdapName(userid);
            String cn = null;
            for (Rdn rdn : ldapname.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    cn = rdn.getValue().toString();
                    logger.debug("uid on DN form. Filtered from {} to {}", userid, cn);
                    break;
                }
            }
            return cn;
        } catch (InvalidNameException e) {
            logger.debug("uid not on DN form. Skipping filter. {}", e.toString());
            return userid;
        }
    }

}
