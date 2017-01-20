package no.nav.sbl.dialogarena.common.cxf;



import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.core.domain.IdentType;
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
import java.util.ArrayList;
import java.util.Base64;

public class ModigOnBehalfOfWithJWTCallbackHandler implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModigOnBehalfOfWithJWTCallbackHandler.class);

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof DelegationCallback) {
                DelegationCallback delegationCallback = (DelegationCallback) callback;
                delegationCallback.setToken(getElement());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    static Element getElement() throws IOException {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        IdentType identType = subjectHandler.getIdentType();

        if (IdentType.Systemressurs.equals(identType)) {
            return null;
        } else if (IdentType.InternBruker.equals(identType)) {
            return lagOnBehalfOfElement(subjectHandler);
        }
        throw new IllegalStateException("wst:OnBehalfOf is only supported for IdentType.Systemressurs and IdentType.InternBruker");
    }

    private static Element lagOnBehalfOfElement(SubjectHandler subjectHandler) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document document;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(getOnBehalfOfString(subjectHandler))));
        } catch (ParserConfigurationException e) {
            logger.error("Exception while getting builder, aborting", e);
            throw new RuntimeException(e);
        } catch (SAXException e) {
            logger.error("Exception while getting OnBehalfOf element, aborting", e);
            throw new RuntimeException(e);
        }

        return document.getDocumentElement();
    }


    private static String getOnBehalfOfString(SubjectHandler subjectHandler) {
        logger.info("subjectHandler={}", subjectHandler);
        JwtCredential jwtCredential = new ArrayList<>( subjectHandler.getSubject().getPublicCredentials(JwtCredential.class)).get(0);
        String base64encodedJTW = Base64.getEncoder().encodeToString(jwtCredential.getToken().getBytes());
        return "<wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"urn:ietf:params:oauth:token-type:jwt\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">" + base64encodedJTW + "</wsse:BinarySecurityToken>";
    }

}
