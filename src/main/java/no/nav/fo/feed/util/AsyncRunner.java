package no.nav.fo.feed.util;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncRunner {

    public static <T> void runAsync(Collection<Runnable> runnables) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            runnables.stream().forEach(c -> pool.submit(c));
        } finally {
            pool.shutdown();
        }
    }

}
