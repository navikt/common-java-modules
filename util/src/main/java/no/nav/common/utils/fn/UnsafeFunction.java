package no.nav.common.utils.fn;

import java.util.function.Function;
import lombok.SneakyThrows;

@FunctionalInterface
public interface UnsafeFunction<T, R> extends Function<T, R> {

    @Override
    @SneakyThrows
    default R apply(T t) {
        return unsafeApply(t);
    }

    R unsafeApply(T t) throws Throwable;
}
