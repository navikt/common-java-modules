package no.nav.modig.security.ws;

import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.security.util.DefaultTimeService;
import no.nav.modig.security.util.TimeService;
import org.apache.wss4j.common.SecurityActionToken;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.action.SAMLTokenSignedAction;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.callback.CallbackHandler;
import java.util.List;

import static java.util.Optional.ofNullable;

public class SAMLTokenCachedSignedAction extends SAMLTokenSignedAction {

    private static Logger logger = LoggerFactory.getLogger(SAMLTokenCachedSignedAction.class);

    private int futureSecondsMargin = 60;
    private boolean cacheAssertion = true;
    private TimeService timeService = new DefaultTimeService();
    private SAMLCallback samlCallback;

    public SAMLTokenCachedSignedAction(boolean cacheAssertion) {
        this.cacheAssertion = cacheAssertion;
    }

    @Override
    public void execute(WSHandler handler, SecurityActionToken actionToDo, RequestData reqData) throws WSSecurityException {
        Document doc = ofNullable(reqData)
                .map(RequestData::getWsDocInfo)
                .map(WSDocInfo::getDocument)
                .orElse(null);

        Element samlAssertion;
        CallbackHandler callbackHandler = null;
        try {
	        callbackHandler = handler.getCallbackHandler(WSHandlerConstants.SAML_CALLBACK_CLASS, WSHandlerConstants.SAML_CALLBACK_REF, reqData);
        } catch(Exception e ) {
        	logger.error("Missing SAMLCallbackHandler. Cannot generate signed SAML. Make sure SystemSAMLOutInterceptor or UserSAMLOutInterceptor is configured", e);
        	throw new WSSecurityException(ErrorCode.SECURITY_ERROR);
        }
        if(callbackHandler==null) {
        	logger.error("Missing SAMLCallbackHandler. Cannot generate signed SAML. Make sure SystemSAMLOutInterceptor or UserSAMLOutInterceptor is configured");
        	throw new WSSecurityException(ErrorCode.SECURITY_ERROR);
        }

   		if (cacheAssertion) {
            // Vi har muligens flere parallelle SOAP-requester.
            // Synkroniserer denne blokken for å forhindre at vi signerer flere SAML-token i parallell.
            synchronized(this) {
                try {
                    samlAssertion = SubjectHandler.getSubjectHandler().getSAMLAssertion();
                } catch (Exception e) {
                    List<Element> list = SubjectHandler.getSubjectHandler().getAllSAMLAssertions();
                    for (Element el : list) {
                        Assertion a = (Assertion) OpenSAMLUtil.fromDom(el);
                        logger.error("One assertion: issuer{} issuerInstant{} id{}", a.getIssuer(), a.getIssueInstant(), a.getID());
                    }
                    throw e;
                }
                if (!hasValidCachedAssertion(samlAssertion)) {
                    samlAssertion = createSamlAssertion(doc, callbackHandler);
                    SubjectHandler.getSubjectHandler().setSAMLAssertion(samlAssertion);
                    logger.debug("Created new SAML token");
                } else {
                    samlAssertion = (Element) doc.importNode(samlAssertion, true);
                    logger.debug("Obtained SAML token from principal");
                }
            }
        } else {
            samlAssertion = createSamlAssertion(doc, callbackHandler);
        }

        prependToHeader(reqData.getSecHeader(), samlAssertion);
    }

    private boolean hasValidCachedAssertion(Element samlAssertion) throws WSSecurityException {
        if (samlAssertion == null) {
            return false;
        }
        Assertion assertion = (Assertion) OpenSAMLUtil.fromDom(samlAssertion);
        DateTime validFrom = null;
        DateTime validTill = null;
        if (assertion.getConditions() != null) {
            validFrom = assertion.getConditions().getNotBefore();
            validTill = assertion.getConditions().getNotOnOrAfter();
        }

        return isBeforeConditionMet(validFrom) && isAfterConditionMet(validTill);
    }

    private boolean isBeforeConditionMet(DateTime validFrom) {
        if (validFrom != null) {
            DateTime currentTime = timeService.getCurrentDateTime();
            logger.debug("Validfrom on saml is: {} and current time now is: {}.",validFrom, currentTime);
            if (!validFrom.isAfter(currentTime)) {
                return true;
            }
        }
        logger.debug("SAML Token condition (Not Before) not met");
        return false;
    }

    private boolean isAfterConditionMet(DateTime validTill) {
        if (validTill != null) {
            DateTime currentTime = timeService.getCurrentDateTime();
            currentTime = currentTime.plusSeconds(futureSecondsMargin);
            logger.debug("Validtill on saml is: {} and current time now is: {}.",validTill, currentTime);
            if (validTill.isAfter(currentTime)) {
                return true;
            }
        }
        logger.debug("SAML Token condition (Not On Or After) not met");
        return false;
    }

    private Element createSamlAssertion(Document doc, CallbackHandler callbackHandler) throws WSSecurityException {

    	SAMLCallback samlCallback = getSAMLCallbackInstanceFromTemplate();
    	
    	SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
    	SamlAssertionWrapper assertionWrapper = new SamlAssertionWrapper(samlCallback);
    	
    	assertionWrapper.signAssertion(
    			samlCallback.getIssuerKeyName(),
    			samlCallback.getIssuerKeyPassword(),
    			samlCallback.getIssuerCrypto(),
    			samlCallback.isSendKeyValue());

		logger.info("Created SAML [" + assertionWrapper.getSignature() + "]");
        return assertionWrapper.toDOM(doc);
    }

	private SAMLCallback getSAMLCallbackInstanceFromTemplate() {
		SAMLCallback samlCallback = new SAMLCallback();
    	samlCallback.setIssuerCrypto(getSamlCallbackTemplate().getIssuerCrypto());
    	samlCallback.setSignAssertion(getSamlCallbackTemplate().isSignAssertion());
    	samlCallback.setIssuerKeyName(getSamlCallbackTemplate().getIssuerKeyName());
    	samlCallback.setIssuerKeyPassword(getSamlCallbackTemplate().getIssuerKeyPassword());
    	samlCallback.setIssuer(getSamlCallbackTemplate().getIssuer());
    	samlCallback.setSendKeyValue(getSamlCallbackTemplate().isSendKeyValue());
		return samlCallback;
	}

    private void prependToHeader(WSSecHeader secHeader, Element element) {
        WSSecurityUtil.prependChildElement(secHeader.getSecurityHeaderElement(), element);
    }

    public void setFutureSecondsMargin(int futureSecondsMargin) {
        this.futureSecondsMargin = futureSecondsMargin;
    }

    public void setCacheAssertion(boolean cacheAssertion) {
        this.cacheAssertion = cacheAssertion;
    }

    public boolean isCacheAssertion() {
        return cacheAssertion;
    }

	public void setSamlCallbackTemplate(SAMLCallback samlCallback) {
		this.samlCallback = samlCallback;
	}

	public SAMLCallback getSamlCallbackTemplate() {
		return samlCallback;
	}


    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }
    
}
