package no.nav.batch;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;

import static no.nav.common.leaderelection.LeaderElection.isNotLeader;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class BatchJob {

    private static String MDC_JOB_ID = "jobId";

    public static void run(Runnable runnable) {
        if (isNotLeader()) {
            log.info("I am not leader, returning...");
            return;
        }
        MDC.put(MDC_JOB_ID, generateId());
        runnable.run();
        MDC.remove(MDC_JOB_ID);
    }

    public static void runAsync(Runnable runnable) {
        if (isNotLeader()) {
            log.info("I am not leader, returning...");
            return;
        }
        MDC.put(MDC_JOB_ID, generateId());
        CompletableFuture.runAsync(runnable);
        MDC.remove(MDC_JOB_ID);
    }
}
