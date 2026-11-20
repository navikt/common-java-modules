package no.nav.common.utils.fn;

import java.util.function.BiConsumer;
import lombok.SneakyThrows;

@FunctionalInterface
public interface UnsafeBiConsumer<T, U> extends BiConsumer<T, U> {

    @Override
    @SneakyThrows
    default void accept(T t, U u) {
        unsafeAccept(t, u);
    }

    void unsafeAccept(T t, U u) throws Throwable;
}
