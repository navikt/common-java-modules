package no.nav.batch;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static no.nav.common.leaderelection.LeaderElection.isNotLeader;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class BatchJob {

    private static String MDC_JOB_ID = "jobId";

    public static Optional<String> run(Runnable runnable) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        String jobId = generateId();
        log.info("Running job with jobId {}", jobId);
        MDC.put(MDC_JOB_ID, jobId);
        runnable.run();
        MDC.remove(MDC_JOB_ID);
        return Optional.of(jobId);
    }

    public static Optional<String> runAsync(Runnable runnable) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        String jobId = generateId();
        log.info("Running job with jobId {}", jobId);
        MDC.put(MDC_JOB_ID, jobId);
        CompletableFuture.runAsync(runnable);
        MDC.remove(MDC_JOB_ID);
        return Optional.of(jobId);
    }
}
