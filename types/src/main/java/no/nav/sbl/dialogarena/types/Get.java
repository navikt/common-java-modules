package no.nav.sbl.dialogarena.types;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

/**
 * Operasjoner som tilbys av ulike typer, wrappet i funksjonsobjekter.
 */
public final class Get {

    public static <T> Transformer<Copyable<T>, T> copy() {
        return new Transformer<Copyable<T>, T>() {
            @Override
            public T transform(Copyable<T> copyable) {
                return copyable.copy();
            }
        };
    }

    public static <T> Transformer<WithId<T>, T> id() {
        return new Transformer<WithId<T>, T>() {
            @Override
            public T transform(WithId<T> withId) {
                return withId.getId();
            }
        };
    }

    public static Transformer<WithKodeverkId, String> kodeverkId() {
        return new Transformer<WithKodeverkId, String>() {
            @Override
            public String transform(WithKodeverkId withKodeverkId) {
                return withKodeverkId.getKodeverkId();
            }
        };
    }

    public static <T> Predicate<WithId<T>> medId(final T id) {
        return new Predicate<WithId<T>>() {
            @Override
            public boolean evaluate(WithId<T> object) {
                return id.equals(object.getId());
            }
        };
    }

    private Get() { }
}
