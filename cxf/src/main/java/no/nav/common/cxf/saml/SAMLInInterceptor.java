package no.nav.common.cxf.saml;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Properties;

/**
 * CXF Soap interceptor som validerer SAML-token og logger inn caller
 */
public class SAMLInInterceptor extends WSS4JInInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(SAMLInInterceptor.class);

    public static final String SUBJECT_REQUEST_ATTRIBUTE_NAME = SAMLInInterceptor.class.getName();

    public SAMLInInterceptor() {
        super();
        setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
    }

    public SAMLInInterceptor(boolean ignore) {
        super(ignore);
        setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
    }

    public SAMLInInterceptor(Map<String, Object> properties) {
        super(properties);
        setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
    }

    @Override
    public Crypto loadSignatureCrypto(RequestData requestData) throws WSSecurityException {

        Properties signatureProperties = new Properties();
        signatureProperties.setProperty("org.apache.wss4j.crypto.merlin.truststore.file", System.getProperty("javax.net.ssl.trustStore"));
        signatureProperties.setProperty("org.apache.wss4j.crypto.merlin.truststore.password", System.getProperty("javax.net.ssl.trustStorePassword"));

        Crypto crypto = CryptoFactory.getInstance(signatureProperties);
        // TODO: Antakelig unødvendig siden Crypto instansen man returnerer er den som blir brukt
//        cryptos.put(WSHandlerConstants.SIG_PROP_REF_ID, crypto);

        return crypto;
    }

    @Override
    public void handleMessage(SoapMessage msg) {

        super.handleMessage(msg);

        SecurityContext sc = (SecurityContext) msg.get(SecurityContext.class.getName());
        if(sc == null) {
        	throw new RuntimeException("Cannot get SecurityContext from SoapMessage");
        }
        SAMLTokenPrincipal samlTokenPrincipal = (SAMLTokenPrincipal) sc.getUserPrincipal();
        if(samlTokenPrincipal == null) {
        	throw new RuntimeException("Cannot get SAMLTokenPrincipal from SecurityContext");
        }
        Assertion assertion = samlTokenPrincipal.getToken().getSaml2();


        logger.debug("SAML Issuer: " + assertion.getIssuer().getValue());
        String subjectNameId = assertion.getSubject().getNameID().getValue();
        logger.debug("SAML Subject: " + subjectNameId);

        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) msg.get("HTTP.REQUEST");
            httpServletRequest.setAttribute(SUBJECT_REQUEST_ATTRIBUTE_NAME, SamlUtils.samlAssertionToSubject(assertion));
        } catch (Exception e) {
            logger.info("Login failed", e);
            WSSecurityException wssecexception = new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, e);
            throw new Fault(wssecexception, wssecexception.getFaultCode()); // NOPMD
        }
    }

}
