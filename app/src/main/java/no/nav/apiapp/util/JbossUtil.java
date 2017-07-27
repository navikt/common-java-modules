package no.nav.apiapp.util;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static no.nav.brukerdialog.security.jaspic.SamAutoRegistration.JASPI_SECURITY_DOMAIN;

public class JbossUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JbossUtil.class);

    public static boolean brukerJaspi() {
        try {
            return ofNullable(SecurityContextAssociation.getSecurityContext()) // på jboss
                    .map(SecurityContext::getSecurityDomain)
                    .map(JASPI_SECURITY_DOMAIN::equals)
                    .orElse(false);
        } catch (NoClassDefFoundError e) {
            // org.jboss-klasser er provided og dermed kun tilgjengelige på jboss
            LOG.warn("mangler jboss-klasse: " + e.getMessage());
            return false;
        }
    }

}
