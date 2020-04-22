package no.nav.common.featuretoggle;


/**
 * @deprecated current recommendation is to use Unleash for feature toggling, see UnleashService
 */
@Deprecated
public interface FeatureToggle {

    boolean erAktiv();

}
