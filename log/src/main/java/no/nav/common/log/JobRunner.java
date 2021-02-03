package no.nav.common.log;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.common.utils.fn.UnsafeRunnable;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

@Slf4j
public class JobRunner {

    public static String run(UnsafeRunnable runnable) {
        final String jobId = IdUtils.generateId();
        run(jobId, jobId, runnable);
        return jobId;
    }

    public static String run(String jobName, UnsafeRunnable runnable) {
        final String jobId = IdUtils.generateId();
        run(jobName, jobId, runnable);
        return jobId;
    }

    /**
     * Runs a job synchronously on the same thread. Adds log tags for job id and name to all logging performed in the runnable.
     * Exceptions thrown from the runnable WILL NOT be caught.
     * @param jobName name of this job
     * @param jobId unique id for this job
     * @param runnable the job that will be ran
     */
    public static void run(String jobName, String jobId, UnsafeRunnable runnable) {
        try {
            MDC.put(MDCConstants.MDC_JOB_ID, jobId);
            MDC.put(MDCConstants.MDC_JOB_NAME, jobName);
            log.info("Job started. jobName={} jobId={}", jobName, jobId);
            runnable.run();
            log.info("Job finished. jobName={} jobId={}", jobName, jobId);
        } catch (Exception e) {
            log.error(format("Job failed. jobName=%s jobId=%s", jobName, jobId), e);
            throw e;
        } finally {
            MDC.remove(MDCConstants.MDC_JOB_ID);
            MDC.remove(MDCConstants.MDC_JOB_NAME);
        }
    }

    public static String runAsync(UnsafeRunnable runnable) {
        final String jobId = IdUtils.generateId();
        runAsync(jobId, jobId, runnable);
        return jobId;
    }

    public static String runAsync(String jobName, UnsafeRunnable runnable) {
        final String jobId = IdUtils.generateId();
        runAsync(jobName, jobId, runnable);
        return jobId;
    }

    /**
     * Runs a job asynchronously on another thread. Adds log tags for job id and name to all logging performed in the runnable.
     * Exceptions thrown from the runnable WILL be caught.
     * @param jobName name of this job
     * @param jobId unique id for this job
     * @param runnable the job that will be ran
     */
    public static void runAsync(String jobName, String jobId, UnsafeRunnable runnable) {
        CompletableFuture.runAsync(() -> run(jobName, jobId, runnable));
    }

}
