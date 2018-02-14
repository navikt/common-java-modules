package no.nav.sbl.featuretoggle.remote;

import no.nav.sbl.featuretoggle.FeatureToggle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.lang.String.format;

public class RemoteFeatureToggle implements FeatureToggle {
    private static Logger log = LoggerFactory.getLogger(RemoteFeatureToggle.class);
    private final RemoteFeatureToggleRepository repository;
    private final String toggleKey;
    private final String[] keyfragments;
    private final boolean erDefaultAktiv;

    public RemoteFeatureToggle(RemoteFeatureToggleRepository repository, String toggleKey) {
        this(repository, toggleKey, false);
    }

    public RemoteFeatureToggle(RemoteFeatureToggleRepository repository, String toggleKey, boolean erDefaultAktiv) {
        this.repository = repository;
        this.toggleKey = toggleKey;
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

        } catch (Throwable e) {
            log.warn(format("Feil ved sjekk av featuretoggle: %s", this.toggleKey), e);
            return erDefaultAktiv;
        }
    }

    private static <S, T> T getOrThrow(Map<S, T> map, S key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(format("Fant ikke %s nøkkel i responsen", key.toString()));
        }
        return map.get(key);
    }
}
