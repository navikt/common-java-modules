package no.nav.sbl.featuretoggle.remote;

import no.nav.sbl.featuretoggle.FeatureToggle;

import java.util.Map;

public class RemoteFeatureToggle implements FeatureToggle {
    private final RemoteFeatureToggleRepository repository;
    private final String[] keyfragments;
    private final boolean erDefaultAktiv;

    public RemoteFeatureToggle(RemoteFeatureToggleRepository repository, String toggleKey) {
        this(repository, toggleKey, false);
    }

    public RemoteFeatureToggle(RemoteFeatureToggleRepository repository, String toggleKey, boolean erDefaultAktiv) {
        this.repository = repository;
        this.keyfragments = toggleKey.split("\\.");
        this.erDefaultAktiv = erDefaultAktiv;
    }

    public final boolean erAktiv() {
        try {
            Map<String, Map<String, Boolean>> features = repository.get();
            String appname = keyfragments[0];
            String togglekey = keyfragments[1];

            Map<String, Boolean> apptoggles = getOrThrow(features, appname);
            return getOrThrow(apptoggles, togglekey);

        } catch (Exception e) {
            return erDefaultAktiv;
        }
    }

    private static <S, T> T getOrThrow(Map<S, T> map, S key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Could not find %s key in response", key.toString()));
        }
        return map.get(key);
    }
}
