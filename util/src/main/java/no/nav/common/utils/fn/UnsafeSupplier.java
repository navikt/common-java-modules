package no.nav.common.utils.fn;

import java.util.function.Supplier;
import lombok.SneakyThrows;

@FunctionalInterface
public interface UnsafeSupplier<T> extends Supplier<T> {

    @Override
    @SneakyThrows
    default T get() {
        return unsafeGet();
    }

    T unsafeGet() throws Throwable;

    static UnsafeSupplier<Void> toVoid(UnsafeRunnable unsafeRunnable) {
        return () -> {
            unsafeRunnable.run();
            return null;
        };
    }
}
