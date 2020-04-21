package no.nav.common.featuretoggle;

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;

/**
 * @deprecated current recommendation is to use Unleash for feature toggling, see UnleashService
 */
@Deprecated
public interface SystemPropertyFeatureToggle extends FeatureToggle {

    default boolean erAktiv() {
        return getOptionalProperty(getSystemVariabelNavn())
                .map(Boolean::parseBoolean)
                .orElseGet(this::erDefaultAktiv);
    }

    String getSystemVariabelNavn();

    boolean erDefaultAktiv();

}
