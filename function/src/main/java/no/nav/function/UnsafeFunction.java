package no.nav.function;

import lombok.SneakyThrows;

import java.util.function.Function;

@FunctionalInterface
public interface UnsafeFunction<T,R> extends Function<T,R> {

    @Override
    @SneakyThrows
    default R apply(T t){
        return unsafeApply(t);
    }
    R unsafeApply(T t) throws Throwable;

}
