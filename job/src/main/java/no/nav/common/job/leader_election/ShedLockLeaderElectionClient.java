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

/**
 * Leader election implemented with [ShedLock](https://github.com/lukas-krecan/ShedLock).
 * Guarantees that 0 or 1 leader is elected at any give moment.
 */
@Slf4j
public class ShedLockLeaderElectionClient implements LeaderElectionClient {

    private final static String LOCK_NAME = "leader-election";

    private final static Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(10);

    private final static Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(10);

    private final static long CHECK_LOCK_INTERVAL_IN_SECONDS = 60;

    // The clock skew is strictly speaking not necessary since we only compare timestamps that has been made by this class.
    // We still add a tiny skew to prevent problems from happening when checking a lock that is just about to expire.
    private final static int CLOCK_SKEW_SECONDS = 5;

    private final static int EXPIRATION_THRESHOLD_SECONDS = 3 * 60; // 3 minutes

    private final LockProvider lockProvider;

    private final ScheduledExecutorService scheduler;

    private final String hostname;

    private volatile boolean isClosed;

    private volatile Instant lockExpiration;

    private volatile SimpleLock lock;

    public ShedLockLeaderElectionClient(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        hostname = EnvironmentUtils.resolveHostName();

        scheduler.scheduleWithFixedDelay(this::keepLockAlive, 0, CHECK_LOCK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public boolean isLeader() {
        return hasAcquiredLock();
    }

    public void close() {
        log.info("Closing ShedLock leader election client...");
        isClosed = true;
        scheduler.shutdown();

        if (lock != null) {
            try {
                lock.unlock();
                lock = null;
                log.info("Leader election lock was released from {}", hostname);
            } catch (Exception e) {
                log.error("Caught exception when unlocking lock during shutdown hook", e);
            }
        }
    }

    private void keepLockAlive() {
        if (hasAcquiredLock()) {
            extendLockIfAboutToExpire();
        } else {
            tryToAcquireLock();
        }
    }

    private void extendLockIfAboutToExpire() {
        Instant now = Instant.now();
        Instant lockAboutToExpire = lockExpiration.minusSeconds(EXPIRATION_THRESHOLD_SECONDS);

        if (now.isAfter(lockAboutToExpire)) {
            log.info("Extending lock which is about to expire. ExtendLockThreshold={} LockExpiresAt={}", lockAboutToExpire, lockExpiration);

            try {
                Optional<SimpleLock> maybeLock = lock.extend(LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);

                if (maybeLock.isPresent()) {
                    setupNewLock(maybeLock.get(), now);
                    log.info("Lock was extended. {} is still the leader", hostname);
                } else {
                    log.warn("Unable to extend lock");
                }
            } catch (UnsupportedOperationException uoe) {
                log.error("ShedLock leader election is being used with a lock provider that does not support lock extension", uoe);
            } catch (Exception e) {
                log.error("Caught exception when extending leader election lock", e);
            }
        }
    }

    private void tryToAcquireLock() {
        Instant createdAt = Instant.now();
        LockConfiguration config = new LockConfiguration(createdAt, LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);

        lockProvider.lock(config).ifPresent((lock) -> {
            setupNewLock(lock, createdAt);
            log.info("Acquired leader election lock. {} is now the leader", hostname);
        });
    }

    private void setupNewLock(SimpleLock newLock, Instant createdAt) {
        lock = newLock;
        lockExpiration = createdAt.plusMillis(LOCK_AT_MOST_FOR.toMillis());
    }

    private boolean hasAcquiredLock() {
        if (isClosed || lockExpiration == null) {
            return false;
        }

        Instant currentTimeWithSkew = Instant.now().plusSeconds(CLOCK_SKEW_SECONDS);

        return currentTimeWithSkew.isBefore(lockExpiration);
    }
}
