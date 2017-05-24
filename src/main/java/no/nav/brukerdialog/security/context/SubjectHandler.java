package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SubjectHandler {
    private static final Logger logger = LoggerFactory.getLogger(SubjectHandler.class);

    public static final String SUBJECTHANDLER_KEY = "no.nav.brukerdialog.security.context.subjectHandlerImplementationClass";
    static final String JBOSS_PROPERTY_KEY = "jboss.home.dir";

    public static SubjectHandler getSubjectHandler() {

        String subjectHandlerImplementationClass;

        if (runningOnJboss()) {
            subjectHandlerImplementationClass = JbossSubjectHandler.class.getName();
        } else {
            subjectHandlerImplementationClass = resolveProperty(SUBJECTHANDLER_KEY);
        }

        if (subjectHandlerImplementationClass == null) {
            throw new RuntimeException(String.format("" +
                            "Du kjører på noe annet enn JBoss. Om du kjører i jetty og test " +
                            "må du konfigurere opp en System property med key %s. " +
                            "Dette kan gjøres på følgende måte: " +
                            "System.setProperty(\"%s\", ThreadLocalSubjectHandler.class.getName());",
                    SUBJECTHANDLER_KEY,
                    SUBJECTHANDLER_KEY
            ));
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
        OidcCredential tokenCredential = getTheOnlyOneInSet(getSubject().getPublicCredentials(OidcCredential.class));
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

        //logging class names to the log to help debug. Cannot log actual objects,
        //since then ID_tokens may be logged
        Set<String> classNames = set.stream()
                .map(Object::getClass)
                .map(Class::getName)
                .collect(Collectors.toSet());
        String errorMessage = "Expected 1 or 0 items, but got " + set.size() + " items. Class of items: " + classNames;
        logger.error(errorMessage);
        throw new IllegalStateException(errorMessage);
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

    @Override
    public String toString() {
        return super.toString();
    }
}
