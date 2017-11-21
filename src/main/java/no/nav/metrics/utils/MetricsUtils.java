package no.nav.metrics.utils;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MetricsUtils {

    public static <S, T> Function<S, T> timed(String navn, Function<S, T> function) {
        return timed(navn, function, null);
    }

    public static <S, T> Function<S, T> timed(String navn, Function<S, T> function, BiConsumer<Timer, Boolean> tagsAppender) {
        return (S s) -> {
            boolean hasFailed = false;
            Timer timer = MetricsFactory.createTimer(navn);
            T t;
            try {
                timer.start();
                t = function.apply(s);
            } catch (Throwable e) {
                hasFailed = true;
                timer.setFailed();
                throw e;
            } finally {
                timer.stop();

                if (tagsAppender != null) {
                    tagsAppender.accept(timer, hasFailed);
                }

                timer.report();
            }
            return t;
        };
    }

    public static <S> S timed(String navn, Supplier<S> supplier) {
        return timed(navn, supplier, null);
    }

    public static <S> S timed(String navn, Supplier<S> supplier, BiConsumer<Timer, Boolean> tagsAppender) {
        return functionToSupplier(timed(navn, supplierToFunction(supplier), tagsAppender)).get();
    }

    public static <S> Consumer<S> timed(String navn, Consumer<S> consumer) {
        return timed(navn, consumer, null);
    }

    public static <S> Consumer<S> timed(String navn, Consumer<S> consumer, BiConsumer<Timer, Boolean> tagsAppender) {
        return functionToConsumer(timed(navn, consumerToFunction(consumer), tagsAppender));
    }

    public static void timed(String navn, Runnable runnable) {
        functionToRunnable(timed(navn, runnableToFunction(runnable))).run();
    }

    public static void timed(String navn, Runnable runnable, BiConsumer<Timer, Boolean> tagsAppender) {
        functionToRunnable(timed(navn, runnableToFunction(runnable), tagsAppender)).run();
    }

    private static <S> Function<S, Void> consumerToFunction(Consumer<S> consumer) {
        return (S s) -> { consumer.accept(s); return null; };
    }

    private static <S> Consumer<S> functionToConsumer(Function<S, Void> function) {
        return function::apply;
    }

    private static <S> Function<Void, S> supplierToFunction(Supplier<S> supplier) {
        return (Void v) -> supplier.get();
    }

    private static <S> Supplier<S> functionToSupplier(Function<Void, S> function) {
        return () -> function.apply(null);
    }

    private static Runnable functionToRunnable(Function<Void, Void> function) {
        return () -> function.apply(null);
    }

    private static Function<Void, Void> runnableToFunction(Runnable runnable) {
        return aVoid -> {
            runnable.run();
            return null;
        };
    }

}
