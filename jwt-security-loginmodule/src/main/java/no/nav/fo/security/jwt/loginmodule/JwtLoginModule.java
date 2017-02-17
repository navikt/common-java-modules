package no.nav.fo.security.jwt.loginmodule;

import no.nav.fo.security.jwt.context.AuthenticationLevelCredential;
import no.nav.fo.security.jwt.context.JwtCredential;
import no.nav.fo.security.jwt.domain.ConsumerId;
import no.nav.fo.security.jwt.domain.IdentType;
import no.nav.fo.security.jwt.domain.SluttBruker;
import no.nav.fo.security.jwt.loginmodule.no.nav.fo.security.jwt.jwks.JwksKeyHandler;
import no.nav.fo.security.jwt.loginmodule.no.nav.fo.security.jwt.jwks.JwtHeader;
import org.jboss.security.SimpleGroup;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Key;
import java.util.*;

/**
 * <p> This <code>LoginModule</code> authenticates users using
 * the custom JWT token.
 */
public class JwtLoginModule implements LoginModule {

    private static final Logger logger = LoggerFactory.getLogger(JwtLoginModule.class);

    private static final JwksKeyHandler jwks = JwksKeyHandler.forIssoHost();

    private Subject subject;
    private CallbackHandler callbackHandler;

    private String jwt;
    private String jwtSubject;
    private int authLevel;
    private ConsumerId consumerId;
    private String identType;
    private List<String> roles;

    private boolean loginSuccess = false;
    private boolean commitSuccess = false;

    private JwtCredential jwtCredential;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        logger.info("Initialize loginmodule");
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        logger.info("Initializing with subject: " + subject + " callbackhandler: " + callbackHandler);
    }

    @Override
    public boolean login() throws LoginException {
        try {
            logger.info("enter login");
            PasswordCallback passwordCallback = new PasswordCallback("Return Jwt as password", false);
            callbackHandler.handle(new Callback[]{passwordCallback});

            jwt = new String(passwordCallback.getPassword());

            JwtHeader header = getHeader(jwt);
            Key key = jwks.getKey(header);
            if (key == null) {
                throw new InvalidJwtException(String.format("Jwt (%s) is not in jwks", header));
            }

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    .setExpectedIssuer(System.getProperty("oidc.iss"))
                    .setVerificationKey(key)
                    .setExpectedAudience("OIDC")
                    .build();

            try {
                JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
                jwtSubject = jwtClaims.getSubject();
                authLevel = 4;
                consumerId = new ConsumerId();
                identType = IdentType.InternBruker.toString();
                roles = new ArrayList<>();  //jwtClaims.getStringListClaimValue("roles");
                logger.info("JWT validation OK:" + jwtClaims);
            } catch (InvalidJwtException e) {
                logger.info("Feil ved validaering av token.", e);
                throw new LoginException(e.toString());
            }

            loginSuccess = true;
            logger.info("Login successful for user " + jwtSubject + " with jwt " + jwt);
            return true;
        } catch (Exception e) {
            jwt = null;
            logger.info("leave login: exception");
            e.printStackTrace();
            throw new LoginException(e.toString());// NOPMD
        }
    }

    private JwtHeader getHeader(String jwt) throws InvalidJwtException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setSkipAllDefaultValidators()
                .setRelaxVerificationKeyValidation()
                .setRelaxDecryptionKeyValidation()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        List<JsonWebStructure> objs = jwtConsumer.process(jwt).getJoseObjects();
        JsonWebStructure wstruct = objs.iterator().next();
        String kid = wstruct.getKeyIdHeaderValue();
        if (kid == null) {
            kid = "";
        }

        return new JwtHeader(kid, wstruct.getHeader("kty"), wstruct.getHeader("use"), wstruct.getAlgorithmHeaderValue());
    }

    @Override
    public boolean commit() throws LoginException {
        logger.info("enter commit");
        if (!subject.isReadOnly()) {
            if (!loginSuccess) {
                cleanupSubject();
                logger.info("leave commit: false");
                return false;
            }

            if (!IdentType.InternBruker.name().equals(identType)) {
                throw new IllegalArgumentException("Støtter bare " + IdentType.InternBruker);
            }
            SluttBruker bruker = SluttBruker.internBruker(jwtSubject);
            subject.getPrincipals().add(bruker);
            subject.getPublicCredentials().add(new AuthenticationLevelCredential(authLevel));

            jwtCredential = new JwtCredential(jwt);
            subject.getPublicCredentials().add(jwtCredential);

            subject.getPrincipals().add(consumerId);

            if (subject.getPublicCredentials(JwtCredential.class).size() != 1) {
                logger.error("We have more than one JwtCrediential: {}", subject.getPublicCredentials(JwtCredential.class).size());
            }

            // Adding JBoss specific Principal to represent groups
            // Used to test rolebased access using @RolesAllowed annotation
            if (roles != null && !roles.isEmpty()) {
                SimpleGroup grp = new SimpleGroup("Roles");
                for (String role : roles) {
                    grp.addMember(new SimpleGroup(role));
                }
                subject.getPrincipals().add(grp);
            }

            commitSuccess = true;
            logger.info("Login committed for user " + jwtSubject + " with jwt " + jwt);
            logger.info("Subject: " + subject);
            return true;
        } else {
            throw new LoginException("Commit failed. Subject is read-only. Cannot add principals and credentials.");
        }
    }


    @Override
    public boolean abort() throws LoginException {
        logger.info("enter abort");
        jwtSubject = null;
        authLevel = -1;
        jwt = null;
        consumerId = null;
        identType = null;

        if (!loginSuccess) {
            logger.info("leave abort: false");
            return false;
        } else if (!commitSuccess) {
            cleanupSubject();
        } else {
            // Login and commit was successful, but someone else failed.
            logout();
        }
        logger.info("leave abort: true");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        logger.info("enter logout");

        loginSuccess = false;
        commitSuccess = false;

        if (!subject.isReadOnly()) {
            cleanupSubject();
            logger.info("leave logout: true");
            return true;
        } else {
            logger.info("leave logout: false. Subject is readonly, cannot cleanup subject.");
            return false;
        }
    }

    private void cleanupSubject() throws LoginException {
        Set<DestroyFailedException> exceptions = new HashSet<>();

        Set<SluttBruker> principals = subject.getPrincipals(SluttBruker.class);
        for (SluttBruker ebp : principals) {
            try {
                String msg = "Logout destroyed and removed " + ebp;
                ebp.destroy();
                subject.getPrincipals().remove(ebp);
                logger.info(msg);
            } catch (DestroyFailedException e) {
                exceptions.add(e);
            }
        }

        Set<ConsumerId> consumerIdPrincipals = subject.getPrincipals(ConsumerId.class);
        for (ConsumerId cip : consumerIdPrincipals) {
            try {
                String msg = "Logout destroyed and removed " + cip;
                cip.destroy();
                subject.getPrincipals().remove(cip);
                logger.info(msg);
            } catch (DestroyFailedException e) {
                exceptions.add(e);
            }
        }


        Set<JwtCredential> jwtCredentials = subject.getPublicCredentials(JwtCredential.class);
        for (JwtCredential jwtCredential : jwtCredentials) {
            try {
                String msg = "Logout destroyed and removed " + jwtCredential;
                jwtCredential.destroy();
                subject.getPublicCredentials().remove(jwtCredential);
                logger.info(msg);

            } catch (DestroyFailedException e) {
                exceptions.add(e);
            }
        }

        Set<AuthenticationLevelCredential> authenticationLevelCredentials = subject.getPublicCredentials(AuthenticationLevelCredential.class);
        for (AuthenticationLevelCredential authenticationLevelCredential : authenticationLevelCredentials) {
            try {
                String msg = "Logout destroyed and removed " + authenticationLevelCredential;
                authenticationLevelCredential.destroy();
                subject.getPublicCredentials().remove(authenticationLevelCredential);
                logger.info(msg);

            } catch (DestroyFailedException e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            logger.info("Logout failed: " + exceptions);
            throw new LoginException("Failed to destroy principals and/or credentials: " + exceptions);
        }

    }

}
