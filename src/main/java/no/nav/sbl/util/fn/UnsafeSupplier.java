package no.nav.sbl.util.fn;

import lombok.SneakyThrows;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface UnsafeSupplier<T> extends Supplier<T> {

    @Override
    @SneakyThrows
    default T get() {
        return unsafeGet();
    }
    T unsafeGet() throws Throwable;

}
