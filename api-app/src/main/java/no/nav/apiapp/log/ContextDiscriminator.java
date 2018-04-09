package no.nav.apiapp.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextDiscriminator.class);
    private static String contextName = System.getProperty("application.name", "default");

    @Override
    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
        return contextName;
    }

    @Override
    public String getKey() {
        return "contextName";
    }

    public static void setContextName(String contextName) {
        String oldContextName = ContextDiscriminator.contextName;
        if (!oldContextName.equals(contextName)) {
            LOGGER.info("changing contextName {} -> {}", oldContextName, contextName);
            ContextDiscriminator.contextName = contextName;
            LOGGER.info("contextName changed {} -> {}", oldContextName, contextName);
        }
    }

}
