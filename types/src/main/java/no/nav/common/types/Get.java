package no.nav.common.types;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

/**
 * Operasjoner som tilbys av ulike typer, wrappet i funksjonsobjekter.
 */
public final class Get {

    public static <T> Transformer<Copyable<T>, T> copy() {
        return copyable -> copyable.copy();
    }

    public static <T> Transformer<WithId<T>, T> id() {
        return withId -> withId.getId();
    }

    public static Transformer<WithKodeverkId, String> kodeverkId() {
        return withKodeverkId -> withKodeverkId.getKodeverkId();
    }

    public static <T> Predicate<WithId<T>> medId(final T id) {
        return object -> id.equals(object.getId());
    }

    private Get() { }
}
