package no.nav.sbl.featuretoggle;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public interface SystemPropertyFeatureToggle extends FeatureToggle {

    default boolean erAktiv() {
        return getOptionalProperty(getSystemVariabelNavn())
                .map(Boolean::parseBoolean)
                .orElseGet(this::erDefaultAktiv);
    }

    String getSystemVariabelNavn();

    boolean erDefaultAktiv();

}
