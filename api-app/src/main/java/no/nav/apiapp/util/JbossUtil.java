package no.nav.apiapp.util;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class JbossUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JbossUtil.class);

    public static Optional<String> getJbossSecurityDomain() {
        try {
            return ofNullable(SecurityContextAssociation.getSecurityContext()) // på jboss
                    .map(SecurityContext::getSecurityDomain);
        } catch (NoClassDefFoundError e) {
            // org.jboss-klasser er provided og dermed kun tilgjengelige på jboss
            LOGGER.warn("mangler jboss-klasse: " + e.getMessage());
            return empty();
        }
    }

}
