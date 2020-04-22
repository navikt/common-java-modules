package no.nav.common.utils.fn;

import lombok.SneakyThrows;

@FunctionalInterface
public interface UnsafeRunnable extends Runnable{

    void runUnsafe() throws Throwable;

    @Override
    @SneakyThrows
    default void run() {
        runUnsafe();
    }
}
