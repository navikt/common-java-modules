package no.nav.sbl.dialogarena.common.cxf.saml;

import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import org.apache.cxf.ws.security.trust.claims.ClaimsCallback;
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

import static no.nav.common.auth.SsoToken.Type.EKSTERN_OPENAM;

public class ClaimsCallbackHandler implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsCallbackHandler.class);

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {

            if (callback instanceof ClaimsCallback) {
                ClaimsCallback claimsCallback = (ClaimsCallback) callback;
                claimsCallback.setClaims(getElement());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private Element getElement() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document document;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(getClaimsString())));
        } catch (ParserConfigurationException e) {
            logger.error("Exception while getting builder, aborting", e);
            throw new RuntimeException(e);
        } catch (SAXException e) {
            logger.error("Exception while getting claims element, aborting", e);
            throw new RuntimeException(e);
        }

        return document.getDocumentElement();
    }

    private String getClaimsString() {
        Subject subject = SubjectHandler.getSubject().orElseThrow(() -> new IllegalStateException("no subject available"));
        String samlToken = subject.getSsoToken(EKSTERN_OPENAM).orElseThrow(() -> new IllegalStateException("no saml token in subject " + subject));
        return "<wst:Claims Dialect=\"http://docs.oasis-open.org/wsfed/authorization/200706/authclaims\" " +
                "xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\" " +
                "xmlns:auth=\"http://docs.oasis-open.org/wsfed/authorization/200706/authclaims\">\n" +
                "    <auth:ClaimType Uri=\"nav:names:claims:openam:tokenid\">\n" +
                "        <auth:Value>" + samlToken + "</auth:Value>\n" +
                "    </auth:ClaimType>\n" +
                "</wst:Claims>";
    }

}
