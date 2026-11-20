package no.nav.common.utils.fn;

import java.util.function.Consumer;
import lombok.SneakyThrows;

@FunctionalInterface
public interface UnsafeConsumer<T> extends Consumer<T> {

    @Override
    @SneakyThrows
    default void accept(T t) {
        unsafeAccept(t);
    }

    void unsafeAccept(T t) throws Throwable;
}
