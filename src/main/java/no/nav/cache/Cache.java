package no.nav.cache;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Cache<KEY, VALUE> {

    VALUE get(KEY key, Function<KEY, VALUE> function);

    default VALUE get(KEY key, Supplier<VALUE> function) {
        return get(key, (k) -> function.get());
    }

}
