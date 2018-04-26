package no.nav.modig.security.ws;

import no.nav.modig.security.ws.attributes.SAMLAttributes;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.*;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NOT A PART OF THE PUBLIC API
 */
public class SAMLCallbackHandler implements CallbackHandler {

    private final SAMLAttributes samlAttributes;
    private static final Logger logger = LoggerFactory.getLogger(SAMLCallbackHandler.class);
    private final int secondsToLive;

    public SAMLCallbackHandler(SAMLAttributes samlAttributes) {
        this(samlAttributes, 1 * 60 * 60); // one hour measured in seconds
    }

    public SAMLCallbackHandler(SAMLAttributes samlAttributes, int secondsToLive) {
        this.samlAttributes = samlAttributes;
        this.secondsToLive = secondsToLive;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (samlAttributes.getUid() == null) {
            throw new RuntimeException("No subject found");
        }

        for (Callback callback : callbacks) {
            SAMLCallback samlCallback = (SAMLCallback) callback;
            samlCallback.setSamlVersion(Version.SAML_20);

            String subjectName = samlAttributes.getUid();
            String subjectQualifier = "www.nav.no";
            SubjectBean subjectBean = new SubjectBean(subjectName, subjectQualifier, SAML2Constants.CONF_BEARER);

            // One-hour validity by default
            ConditionsBean conditionsBean = new ConditionsBean();
            DateTime notBefore = new DateTime();
            conditionsBean.setNotBefore(notBefore);
            conditionsBean.setNotAfter(notBefore.plusSeconds(secondsToLive));
            samlCallback.setConditions(conditionsBean);

            samlCallback.setSubject(subjectBean);

            samlCallback.setIssuer(samlAttributes.getConsumerId());

            List<AttributeBean> attributeBeans = new ArrayList<>();
            attributeBeans.add(createAttributeBean("authenticationLevel", samlAttributes.getAuthenticationLevel()));
            attributeBeans.add(createAttributeBean("identType", samlAttributes.getIdentType()));
            attributeBeans.add(createAttributeBean("consumerId", samlAttributes.getConsumerId()));
            AttributeStatementBean attrStatementBean = new AttributeStatementBean();
            attrStatementBean.setSamlAttributes(attributeBeans);

            samlCallback.setAttributeStatementData(Collections.singletonList(attrStatementBean));
            
            logger.debug("Added SAML token for subject {}, identType {}, authenticationLevel {}, consumerId {}", subjectName, samlAttributes.getIdentType(), samlAttributes.getAuthenticationLevel(),
                    samlAttributes.getConsumerId());

        }
    }

    private AttributeBean createAttributeBean(String name, String value) {
        AttributeBean attributeBean = new AttributeBean();
        attributeBean.setQualifiedName(name);
        attributeBean.setAttributeValues(Collections.singletonList(value));
        return attributeBean;
    }
}
