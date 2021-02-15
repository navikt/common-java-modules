package no.nav.common.featuretoggle;

import no.finn.unleash.UnleashContext;

public interface UnleashClient {

    boolean isEnabled(String toggleName);

    boolean isEnabled(String toggleName, UnleashContext unleashContext);

}
