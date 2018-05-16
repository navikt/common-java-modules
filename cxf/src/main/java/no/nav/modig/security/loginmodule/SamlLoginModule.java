package no.nav.modig.security.loginmodule;


import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.SAMLAssertionCredential;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.IdentType;
import no.nav.modig.core.domain.SluttBruker;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p> This <code>LoginModule</code> authenticates users using
 * the custom SAML token.
 */
public class SamlLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;

    private SamlInfo samlInfo;
    private Assertion samlAssertion;

    private boolean loginSuccess = false;
    private boolean commitSuccess = false;

    private SluttBruker sluttbrukerPrincipal;
    private AuthenticationLevelCredential authenticationLevelCredential;
    private SAMLAssertionCredential samlAssertionCredential;
    private ConsumerId consumerId;

    private static Logger logger = LoggerFactory.getLogger(SamlLoginModule.class);

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        logger.debug("Initialize loginmodule");
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        logger.debug("Initializing with subject: " + subject +
                " callbackhandler: " + callbackHandler);
    }

    @Override
    public boolean login() throws LoginException {
        try {
            logger.debug("enter login");
            PasswordCallback passwordCallback = new PasswordCallback("Return SAML-assertion as password", false);
            callbackHandler.handle(new Callback[] { passwordCallback });

            samlAssertion = SamlUtils.toSamlAssertion(new String(passwordCallback.getPassword()));
            samlInfo = SamlUtils.getSamlInfo(samlAssertion);
            loginSuccess = true;
            logger.debug("Login successful for user " + samlInfo.getUid() + " with authentication level " + samlInfo.getAuthLevel());
            return true;
        } catch (Exception e) {
            samlAssertion = null;
            samlInfo = null;
            logger.debug("leave login: exception");
            throw new LoginException(e.toString());// NOPMD
        }
    }

    @Override
    public boolean commit() throws LoginException {
        logger.debug("enter commit");
        if (!loginSuccess) {
            cleanSamlInfo();
            logger.debug("leave commit: false");
            return false;
        }

        if(samlInfo.getIdentType() != null) {
        	sluttbrukerPrincipal = new SluttBruker(samlInfo.getUid(), getIdentType());
        	subject.getPrincipals().add(sluttbrukerPrincipal);
        }
        
        if(samlInfo.getAuthLevel() >= 0) {
        	authenticationLevelCredential = new AuthenticationLevelCredential(samlInfo.getAuthLevel());
        	subject.getPublicCredentials().add(authenticationLevelCredential);
        }

        if(samlInfo.getConsumerId() != null) {
        	consumerId = new ConsumerId(samlInfo.getConsumerId());
        	subject.getPrincipals().add(consumerId);
        }
        
        samlAssertionCredential = new SAMLAssertionCredential(samlAssertion.getDOM());
        subject.getPublicCredentials().add(samlAssertionCredential);
        
        if (subject.getPublicCredentials(SAMLAssertionCredential.class).size() != 1) {
            logger.error("We have more than one SAMLAssertionCrediential: {}", subject.getPublicCredentials(SAMLAssertionCredential.class).size());
        }
        commitSuccess = true;
        logger.debug("Login committed for subject with uid: " + samlInfo.getUid() +
                " authentication level: " + samlInfo.getAuthLevel() +
                " and consumerId: " + consumerId);
        return true;
    }


    @Override
    public boolean abort() throws LoginException {
        logger.debug("enter abort");
        if (!loginSuccess) {
            logger.debug("leave abort: false");
            return false;
        } else if (!commitSuccess) {
            cleanSamlInfo();
        } else {
            // Login and commit was successful, but someone else failed.
            logout();
        }
        logger.debug("leave abort: true");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        logger.debug("enter logout");
        subject.getPrincipals().remove(sluttbrukerPrincipal);
        subject.getPrincipals().remove(consumerId);

        Iterator<Object> it = subject.getPublicCredentials().iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof AuthenticationLevelCredential ||
                    o instanceof SAMLAssertionCredential) {
                it.remove();
                logger.debug("Logout removed " + o.getClass().getName());
            }
        }
        cleanSamlInfo();

        loginSuccess = false;
        commitSuccess = false;
        logger.debug("leave logout: true");
        return true;
    }

    private void cleanSamlInfo() throws LoginException {
        Set<DestroyFailedException> exceptions = new HashSet<>();
        try {
            if (sluttbrukerPrincipal != null) {
                sluttbrukerPrincipal.destroy();
            }
        } catch (DestroyFailedException e) {
            exceptions.add(e);
        }
        try {
            if (consumerId != null) {
                consumerId.destroy();
            }
        } catch (DestroyFailedException e) {
            exceptions.add(e);
        }
        try {
            if (authenticationLevelCredential != null) {
                authenticationLevelCredential.destroy();
            }
        } catch (DestroyFailedException e) {
            exceptions.add(e);
        }
        try {
            if (samlAssertionCredential != null) {
                samlAssertionCredential.destroy();
            }
        } catch (DestroyFailedException e) {
            exceptions.add(e);
        }

        if (!exceptions.isEmpty()) {
            logger.debug("cleanSamlInfo failed: " + exceptions);
            throw new LoginException("Failed to destroy principals and/or credentials: " + exceptions);
        }

        subject = null;
        samlInfo = null;
        samlAssertion = null;
        sluttbrukerPrincipal = null;
        consumerId = null;
        authenticationLevelCredential = null;
        samlAssertionCredential = null;
    }

    private IdentType getIdentType() throws LoginException {
        IdentType identType;
        try {
            identType = IdentType.valueOf(samlInfo.getIdentType());
        } catch (IllegalArgumentException e) {
            LoginException le = new LoginException("Could not commit. Unknown ident type: " + samlInfo.getIdentType() + " " + e);
            le.initCause(e);
            throw le;
        }
        return identType;
    }
}
