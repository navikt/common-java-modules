package no.nav.sbl.util.fn;

import lombok.SneakyThrows;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface UnsafeConsumer<T> extends Consumer<T> {

    @Override
    @SneakyThrows
    default void accept(T t) {
        unsafeAccept(t);
    }
    void unsafeAccept(T t) throws Throwable;

}
