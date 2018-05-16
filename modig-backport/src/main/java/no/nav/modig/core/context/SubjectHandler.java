package no.nav.modig.core.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.IdentType;
import no.nav.modig.core.domain.SluttBruker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public abstract class SubjectHandler {
    public static final String SUBJECTHANDLER_KEY = "no.nav.modig.core.context.subjectHandlerImplementationClass";
    private static final Logger logger = LoggerFactory.getLogger(SubjectHandler.class);

    public static SubjectHandler getSubjectHandler() {

        String subjectHandlerImplementationClass = resolveProperty(SUBJECTHANDLER_KEY);

        if (subjectHandlerImplementationClass == null) {
            throw new RuntimeException("Du kjører på noe annet enn JBoss, WAS eller WLS. Om du kjører i jetty og test " +
                    "må du konfigurere opp en System property med key no.nav.modig.core.context.subjectHandlerImplementationClass. " +
                    "Dette kan gjøres på følgende måte: " +
                    "System.setProperty(\"no.nav.modig.core.context.subjectHandlerImplementationClass\", ThreadLocalSubjectHandler.class.getName());");
        }

        try {
            Class<?> clazz = Class.forName(subjectHandlerImplementationClass);
            return (SubjectHandler) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not configure platform dependent subject handler", e);
        }
    }

    public abstract Subject getSubject();

    public String getUid() {
        if (!hasSubject()) {
            return null;
        }

        SluttBruker sluttBruker = getTheOnlyOneInSet(getSubject().getPrincipals(SluttBruker.class));
        if (sluttBruker != null) {
            return sluttBruker.getName();
        }

        String userId = getUidFromSAMLToken();
        if (userId != null) {
            return userId;
        }

        return null;
    }

    public IdentType getIdentType() {
        if (!hasSubject()) {
            return null;
        }

        SluttBruker sluttBruker = getTheOnlyOneInSet(getSubject().getPrincipals(SluttBruker.class));
        if (sluttBruker != null) {
            return sluttBruker.getIdentType();
        }

        IdentType identType = getIdentTypeFromSAMLToken();
        if (identType != null) {
            return identType;
        }

        return null;
    }

    public Integer getAuthenticationLevel() {
        if (!hasSubject()) {
            return null;
        }

        AuthenticationLevelCredential authenticationLevelCredential = getTheOnlyOneInSet(getSubject().getPublicCredentials(AuthenticationLevelCredential.class));
        if (authenticationLevelCredential != null) {
            return authenticationLevelCredential.getAuthenticationLevel();
        }

        Integer authenticationLevel = getAuthenticationLevelFromSAMLToken();
        if (authenticationLevel != null) {
            return authenticationLevel;
        }

        return null;
    }

    public String getConsumerId() {
        if (!hasSubject()) {
            return null;
        }

        ConsumerId consumerId = getTheOnlyOneInSet(getSubject().getPrincipals(ConsumerId.class));
        if (consumerId != null) {
            return consumerId.getConsumerId();
        }

        String consumerIdString = getConsumerIdFromSAMLToken();
        if (consumerIdString != null) {
            return consumerIdString;
        }

        return null;
    }

    public String getEksternSsoToken() {
        if (!hasSubject()) {
            return null;
        }
        OpenAmTokenCredential tokenCredential = getTheOnlyOneInSet(getSubject().getPublicCredentials(OpenAmTokenCredential.class));
        return tokenCredential != null ? tokenCredential.getOpenAmToken() : null;
    }

    public Element getSAMLAssertion() {
        if (!hasSubject()) {
            return null;
        }

        if (getSubject() != null) {
            SAMLAssertionCredential credential = getTheOnlyOneInSet(getSubject().getPublicCredentials(SAMLAssertionCredential.class));
            if (credential != null) {
                return credential.getElement();
            }
        }
        return null;
    }


    public List<Element> getAllSAMLAssertions() {
        List<Element> list = new ArrayList<Element>();

        for (SAMLAssertionCredential credential : getSubject().getPublicCredentials(SAMLAssertionCredential.class)) {
            list.add(credential.getElement());
        }
        return list;
    }


    /**
     * Todo: Denne skal fjernes når vi slutter å self-signe sertifikater.
     */
    public void setSAMLAssertion(Element element) {
        if (!hasSubject()) {
            return;
        }
        //fjerne gamle SAMLAssertionCredentials (som er utløpt)
        getSubject().getPublicCredentials().removeAll(getSubject().getPublicCredentials(SAMLAssertionCredential.class));
        getSubject().getPublicCredentials().add(new SAMLAssertionCredential(element));
    }

    protected IdentType getIdentTypeFromSAMLToken() {
        return null;
    }

    protected Integer getAuthenticationLevelFromSAMLToken() {
        return null;
    }

    protected String getUidFromSAMLToken() {
        return null;
    }

    protected String getConsumerIdFromSAMLToken() {
        return null;
    }

    private <T> T getTheOnlyOneInSet(Set<T> set) {
        if (set.isEmpty()) {
            return null;
        }

        T first = set.iterator().next();
        if (set.size() == 1) {
            return first;
        }

        logger.error("expected 1 (or zero) items, got "+set.size()+", listing them:");
        for(T item : set){
        	logger.error(item.toString());
        }
        throw new IllegalStateException("To many (" + set.size() + ") " + first.getClass().getName() + ". Should be either 1 (logged in) og 0 (not logged in)");
    }

    private static String resolveProperty(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            logger.debug("Setting " + key + "={} from System.properties", value);
        }
        return value;
    }

    private Boolean hasSubject() {
        return getSubject() != null;
    }
}
