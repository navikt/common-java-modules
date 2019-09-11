package no.nav.batch;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static no.nav.common.leaderelection.LeaderElection.isNotLeader;
import static no.nav.util.common.IdUtils.generateId;

@Slf4j
public class BatchJob {

    private static String MDC_JOB_ID = "jobId";

    public static Optional<String> runAsyncOnLeader(Runnable runnable) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        return Optional.of(runAsync(runnable));
    }


    public static String runAsync(Runnable runnable) {
        String jobId = generateId();
        log.info("Running job with jobId {}", jobId);
        CompletableFuture.runAsync(() -> {
            MDC.put(MDC_JOB_ID, jobId);
            runnable.run();
            MDC.remove(MDC_JOB_ID);
        });
        return jobId;
    }
}
