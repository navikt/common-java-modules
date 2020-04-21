package no.nav.common.utils;

import no.nav.common.utils.fn.UnsafeBiConsumer;
import no.nav.common.utils.fn.UnsafeConsumer;
import no.nav.common.utils.fn.UnsafeFunction;
import no.nav.common.utils.fn.UnsafeSupplier;

import java.util.Optional;
import java.util.function.*;

public class FunctionalUtils {

    public static <T, U> BiConsumer<T, U> sneaky(UnsafeBiConsumer<T, U> unsafeBiConsumer) {
        return unsafeBiConsumer;
    }

    public static <T> Consumer<T> sneaky(UnsafeConsumer<T> unsafeBiConsumer) {
        return unsafeBiConsumer;
    }

    public static <T> Supplier<T> sneaky(UnsafeSupplier<T> unsafeBiConsumer) {
        return unsafeBiConsumer;
    }

    public static <T, R> Function<T, R> sneakyFunction(UnsafeFunction<T, R> function) {
        return function;
    }

    public static <A, B, R> Function<A, Optional<R>> combineOptional(Optional<B> optionalB, BiFunction<A, B, R> biConsumer) {
        return a -> optionalB.map(b -> biConsumer.apply(a, b));
    }

}
