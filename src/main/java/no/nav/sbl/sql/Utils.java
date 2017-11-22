package no.nav.sbl.sql;

import lombok.SneakyThrows;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Utils {

    @SneakyThrows
    public static <T> T timedPreparedStatement(String sql, Callable<T> task, Consumer<Timer> additions) {
        Timer timer = MetricsFactory.createTimer(dbTimerNavn(sql));
        try {
            timer.start();
            return task.call();
        } catch (Throwable e) {
            timer.setFailed();
            throw e;
        } finally {
            timer.stop();
            if (additions != null) {
                additions.accept(timer);
            }
            timer.report();
        }
    }

    public static <T> T timedPreparedStatement(String name, Callable<T> task) {
        return timedPreparedStatement(name, task, null);
    }

    private static String dbTimerNavn(String sql) {
        return (sql + ".db").replaceAll("[^\\w]","-");
    }



}
