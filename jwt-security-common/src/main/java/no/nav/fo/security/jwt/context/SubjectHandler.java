package no.nav.fo.security.jwt.context;


import no.nav.fo.security.jwt.domain.ConsumerId;
import no.nav.fo.security.jwt.domain.IdentType;
import no.nav.fo.security.jwt.domain.SluttBruker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.util.Set;

public abstract class SubjectHandler {
    private static final Logger logger = LoggerFactory.getLogger(SubjectHandler.class);

    static final String SUBJECTHANDLER_KEY = "no.nav.modig.core.context.subjectHandlerImplementationClass";
    static final String JBOSS_PROPERTY_KEY = "jboss.home.dir";

    public static SubjectHandler getSubjectHandler() {

        String subjectHandlerImplementationClass;

        if (runningOnJboss()) {
            subjectHandlerImplementationClass = JbossSubjectHandler.class.getName();
            logger.debug("Detected running on JBoss Application Server. Using: " + subjectHandlerImplementationClass);
        } else {
            subjectHandlerImplementationClass = resolveProperty(SUBJECTHANDLER_KEY);
        }

        if (subjectHandlerImplementationClass == null) {
            throw new RuntimeException("Du kjører på noe annet enn JBoss. Om du kjører i jetty og test " +
                    "må du konfigurere opp en System property med key no.nav.modig.core.context.subjectHandlerImplementationClass. " +
                    "Dette kan gjøres på følgende måte: " +
                    "System.setProperty(\"no.nav.modig.core.context.subjectHandlerImplementationClass\", ThreadLocalSubjectHandler.class.getName());");
        }

        try {
            Class<?> clazz = Class.forName(subjectHandlerImplementationClass);
            return (SubjectHandler) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
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

        return null;
    }

    public String getInternSsoToken() {
        if (!hasSubject()) {
            return null;
        }
        JwtCredential tokenCredential = getTheOnlyOneInSet(getSubject().getPublicCredentials(JwtCredential.class));
        return tokenCredential != null ? tokenCredential.getToken() : null;
    }

    public Integer getAuthenticationLevel() {
        if (!hasSubject()) {
            return null;
        }

        AuthenticationLevelCredential authenticationLevelCredential = getTheOnlyOneInSet(getSubject().getPublicCredentials(AuthenticationLevelCredential.class));
        if (authenticationLevelCredential != null) {
            return authenticationLevelCredential.getAuthenticationLevel();
        }

        return null;
    }

    String getConsumerId() {
        if (!hasSubject()) {
            return null;
        }

        ConsumerId consumerId = getTheOnlyOneInSet(getSubject().getPrincipals(ConsumerId.class));
        if (consumerId != null) {
            return consumerId.getConsumerId();
        }

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

        logger.error("expected 1 (or zero) items, got " + set.size() + ", listing them:");
        for (T item : set) {
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

    private static boolean runningOnJboss() {
        return existsInProperties(JBOSS_PROPERTY_KEY);
    }

    private static boolean existsInProperties(String key) {
        return System.getProperties().containsKey(key);
    }

    private Boolean hasSubject() {
        return getSubject() != null;
    }
}
