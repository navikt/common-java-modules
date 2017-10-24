package no.nav.sbl.util;

import no.nav.sbl.util.fn.UnsafeBiConsumer;
import no.nav.sbl.util.fn.UnsafeConsumer;
import no.nav.sbl.util.fn.UnsafeFunction;
import no.nav.sbl.util.fn.UnsafeSupplier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

}
