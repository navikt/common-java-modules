package no.nav.common.featuretoggle;

import no.finn.unleash.UnleashContext;
import no.nav.common.health.HealthCheck;

public interface UnleashClient extends HealthCheck {

    boolean isEnabled(String toggleName);

    boolean isEnabled(String toggleName, UnleashContext unleashContext);

}
