package no.nav.modig.security.ws;

import no.nav.modig.security.util.PropertySource;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.common.saml.SAMLCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.wss4j.common.crypto.Merlin.KEYSTORE_FILE;
import static org.apache.wss4j.common.crypto.Merlin.KEYSTORE_PASSWORD;
import static org.apache.wss4j.common.crypto.Merlin.PREFIX;
import static org.apache.wss4j.common.ConfigurationConstants.ACTION;
import static org.apache.wss4j.common.ConfigurationConstants.MUST_UNDERSTAND;

/**
 * Baseklasse for SAML out interceptors
 *
 * NOT A PART OF THE PUBLIC API
 */
public abstract class AbstractSAMLOutInterceptor extends WSS4JOutInterceptor {

    public static final String SYSTEM_PROPERTY_APPCERT_ALIAS = "no.nav.modig.security.appcert.keystorealias";
    public static final String SYSTEM_PROPERTY_APPCERT_PASSWORD = "no.nav.modig.security.appcert.password";
    public static final String SYSTEM_PROPERTY_APPCERT_FILE = "no.nav.modig.security.appcert.keystore";
    public static final String SYSTEM_PROPERTY_APPCERT_ISSUER = "no.nav.modig.security.appcert.issuer";
    public static final String SAML_ISSUER_KEY_NAME = "org.apache.ws.security.saml.issuer.key.name";
    public static final String SAML_ISSUER_KEY_PASSWORD = "org.apache.ws.security.saml.issuer.key.password";
    public static final String SAML_ISSUER_NAME = "org.apache.ws.security.saml.issuer";
    public static final String SAML_SEND_KEY_VALUE = "org.apache.ws.security.saml.issuer.sendKeyValue";

    private PropertySource securityOutInterceptorProperties = new PropertySource("securityOutInterceptor.properties");

    static final String DEFAULTSENDKEYVALUE = "false";
    static final String DEFAULTMUSTUNDERSTAND = "false";

    private static SAMLCallback samlCallbackTemplate;
    
    private final boolean cacheAssertion;

    public AbstractSAMLOutInterceptor(boolean cacheAssertion) {
        super();
        this.cacheAssertion = cacheAssertion;
        setNAVProperties();
        setAllowMTOM(true);
    }

    public AbstractSAMLOutInterceptor(boolean cacheAssertion, Map<String, Object> props) {
        super(props);
        this.cacheAssertion = cacheAssertion;
        setNAVProperties();
    }

    private void setNAVProperties() {
        Crypto crypto = createCrypto(securityOutInterceptorProperties);
        SAMLCallback samlCallback = createSamlCallbackTemplate(crypto, securityOutInterceptorProperties);
        Map<Integer, Object> actionmap = createActionMap(samlCallback, cacheAssertion);
        Map<String, Object> configmap = createConfigProperties(actionmap, securityOutInterceptorProperties);

        setProperties(configmap);
    }

    static Map<String, Object> createConfigProperties(Map<Integer, Object> actionmap, PropertySource samlProperties) {
        HashMap<String, Object> configMap = new HashMap<>();
        configMap.put(ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
        configMap.put(WSS4J_ACTION_MAP, actionmap);
        configMap.put(MUST_UNDERSTAND, samlProperties.getProperty(MUST_UNDERSTAND, DEFAULTMUSTUNDERSTAND));
        return configMap;
    }

    static Map<Integer, Object> createActionMap(SAMLCallback samlCallback, boolean assertionCached) {
        SAMLTokenCachedSignedAction action = new SAMLTokenCachedSignedAction(assertionCached);
        action.setSamlCallbackTemplate(samlCallback);

        HashMap<Integer, Object> actionmap = new HashMap<>();
        actionmap.put(WSConstants.ST_SIGNED, action);
        return actionmap;
    }

    static SAMLCallback createSamlCallbackTemplate(Crypto crypto, PropertySource samlProperties) {
    	
    	samlCallbackTemplate = new SAMLCallback();
    	samlCallbackTemplate.setIssuerCrypto(crypto);
    	samlCallbackTemplate.setSignAssertion(true);
    	samlCallbackTemplate.setIssuerKeyName(samlProperties.getProperty(SAML_ISSUER_KEY_NAME, System.getProperty(SYSTEM_PROPERTY_APPCERT_ALIAS, "app-key")));
    	samlCallbackTemplate.setIssuerKeyPassword(samlProperties.getProperty(SAML_ISSUER_KEY_PASSWORD, System.getProperty(SYSTEM_PROPERTY_APPCERT_PASSWORD)));
    	samlCallbackTemplate.setIssuer(samlProperties.getProperty(SAML_ISSUER_NAME, System.getProperty(SYSTEM_PROPERTY_APPCERT_ISSUER, "app-key")));
    	samlCallbackTemplate.setSendKeyValue(Boolean.valueOf(samlProperties.getProperty(SAML_SEND_KEY_VALUE, DEFAULTSENDKEYVALUE)));
    	
    	return samlCallbackTemplate;
    }
    
    static SAMLCallback getSamlCallbackTemplate() {
    	return samlCallbackTemplate;
    }

    static Merlin createCrypto(PropertySource cryptoProperties) {
        Merlin crypto = new Merlin();

        Properties keystoreProps = new Properties();
        keystoreProps.setProperty(PREFIX + KEYSTORE_FILE, cryptoProperties.getProperty(PREFIX + KEYSTORE_FILE, System.getProperty(SYSTEM_PROPERTY_APPCERT_FILE)));
        keystoreProps.setProperty(PREFIX + KEYSTORE_PASSWORD, cryptoProperties.getProperty(PREFIX + KEYSTORE_PASSWORD, System.getProperty(SYSTEM_PROPERTY_APPCERT_PASSWORD)));

        try {
            crypto.loadProperties(keystoreProps, AbstractSAMLOutInterceptor.class.getClassLoader(), null);
        } catch (Exception e) {
            throw new RuntimeException("Error while loading crypto", e);
        }
        return crypto;
    }

    protected abstract SAMLCallbackHandler getCallbackHandler();

}
