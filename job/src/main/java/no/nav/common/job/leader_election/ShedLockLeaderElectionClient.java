package no.nav.common.job.leader_election;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.utils.EnvironmentUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShedLockLeaderElectionClient implements LeaderElectionClient {

    private final static String LOCK_NAME = "leader-election";

    private final static Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(5);

    private final static Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(5);

    private final static int CLOCK_SKEW_SECONDS = 5;

    private final static int LOCK_EXTENSION_RETRY_SECONDS = 5;

    private final static int LOCK_EXTENSION_BEFORE_EXPIRATION_SECONDS = 30;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final LockProvider lockProvider;

    private volatile boolean hasShutDown;

    private volatile Instant lockExpiration;

    private volatile SimpleLock lock;

    public ShedLockLeaderElectionClient(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
    }

    @Override
    public boolean isLeader() {
        if (hasAcquiredLock()) {
            return true;
        }

        Instant createdAt = Instant.now();
        Optional<SimpleLock> maybeLock = lockProvider.lock(createLockConfig(createdAt));

        if (maybeLock.isPresent()) {
            setupNewLock(maybeLock.get(), createdAt);
            return true;
        }

        return false;
    }

    private void extendLock() {
        if (lock != null) {
            try {
                Instant createdAt = Instant.now();
                Optional<SimpleLock> extendedLock = lock.extend(LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);

                if (extendedLock.isPresent()) {
                    setupNewLock(extendedLock.get(), createdAt);
                } else {
                    log.warn("Unable to extend lock currentTime={} lockExpiration={}", Instant.now(), lockExpiration);

                    if (hasAcquiredLock()) {
                        Instant nextExtend = createdAt.plusSeconds(LOCK_EXTENSION_RETRY_SECONDS);
                        log.info("Retrying extension of lock at {}", nextExtend);
                        executorService.schedule(this::extendLock, nextExtend.toEpochMilli(), TimeUnit.MILLISECONDS);
                    } else {
                        log.info("Lock is not held anymore. Extension of lock is halted until new lock is acquired.");
                    }
                }
            } catch (UnsupportedOperationException uoe) {
                log.error("{} is being used with a lock provider that does not support lock extension", getClass().getCanonicalName());
            } catch (Exception e) {
                log.error("Caught exception when extending leader election lock", e);
            }
        }
    }

    private void setupNewLock(SimpleLock newLock, Instant createdAt) {
        lock = newLock;
        lockExpiration = createdAt.plusMillis(LOCK_AT_MOST_FOR.toMillis());

        Instant lockExtensionStartTime = lockExpiration.minusSeconds(LOCK_EXTENSION_BEFORE_EXPIRATION_SECONDS);
        executorService.schedule(this::extendLock, lockExtensionStartTime.toEpochMilli(), TimeUnit.MILLISECONDS);

        log.info("Acquired leader-election lock. {} is now the leader", EnvironmentUtils.resolveHostName());
    }

    private void shutdownHook() {
        hasShutDown = true;

        if (lock != null) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.error("Caught exception when unlocking lock during shutdown hook", e);
            }
        }
    }

    private boolean hasAcquiredLock() {
        if (lockExpiration == null || hasShutDown) {
            return false;
        }

        // Add clock skew to reduce the chance that more than 1 leader is elected simultaneously
        Instant currentTimeWithSkew = Instant.now().plusSeconds(CLOCK_SKEW_SECONDS);

        boolean hasLock = currentTimeWithSkew.isBefore(lockExpiration);

        // Cleanup the lock if it is no longer held
        if (!hasLock) {
            lock = null;
            lockExpiration = null;
        }

        return hasLock;
    }

    private LockConfiguration createLockConfig(Instant createdAt) {
        return new LockConfiguration(createdAt, LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);
    }

}
