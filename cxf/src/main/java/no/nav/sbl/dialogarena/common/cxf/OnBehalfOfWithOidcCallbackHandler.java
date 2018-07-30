package no.nav.sbl.dialogarena.common.cxf;


import no.nav.common.auth.Subject;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;

import static no.nav.common.auth.SsoToken.Type.OIDC;

public class OnBehalfOfWithOidcCallbackHandler implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(OnBehalfOfWithOidcCallbackHandler.class);

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof DelegationCallback) {
                DelegationCallback delegationCallback = (DelegationCallback) callback;
                delegationCallback.setToken(lagOnBehalfOfElement());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private static Element lagOnBehalfOfElement() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document document;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(getOnBehalfOfString())));
        } catch (ParserConfigurationException e) {
            logger.error("Exception while getting builder, aborting", e);
            throw new RuntimeException(e);
        } catch (SAXException e) {
            logger.error("Exception while getting OnBehalfOf element, aborting", e);
            throw new RuntimeException(e);
        }

        return document.getDocumentElement();
    }


    private static String getOnBehalfOfString() {
        Subject subject = no.nav.common.auth.SubjectHandler.getSubject().orElseThrow(() -> new IllegalStateException("no subject available"));
        String oidcToken = subject.getSsoToken(OIDC).orElseThrow(() -> new IllegalStateException("no oidc token in subject " + subject));
        String base64encodedJTW = Base64.getEncoder().encodeToString(oidcToken.getBytes());
        return "<wsse:BinarySecurityToken" +
                " EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\"" +
                " ValueType=\"urn:ietf:params:oauth:token-type:jwt\"" +
                " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"" +
                ">"
                + base64encodedJTW
                + "</wsse:BinarySecurityToken>";
    }

}
