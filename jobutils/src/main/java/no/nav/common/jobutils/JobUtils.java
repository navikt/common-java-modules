package no.nav.common.jobutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.net.InetAddress.getLocalHost;
import static no.nav.common.leaderelection.LeaderElectionHttpClient.isNotLeader;
import static no.nav.common.utils.IdUtils.generateId;

public class JobUtils {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static String MDC_JOB_ID = "jobId";

    public static Optional<RunningJob> runAsyncJobOnLeader(Runnable runnable) {
        if (isNotLeader()) {
            return Optional.empty();
        }
        RunningJob job = runAsyncJob(runnable);
        return Optional.of(job);
    }

    public static RunningJob runAsyncJob(Runnable runnable) {

        String jobId = generateId();

        log.info("Running job with jobId {}", jobId);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            MDC.put(MDC_JOB_ID, jobId);
            runnable.run();
            MDC.remove(MDC_JOB_ID);
        });

        future.exceptionally(e -> {
            throw new RuntimeException(e);
        });

        String podName;
        try {
            podName = getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return new RunningJob(jobId, podName);
    }
}

