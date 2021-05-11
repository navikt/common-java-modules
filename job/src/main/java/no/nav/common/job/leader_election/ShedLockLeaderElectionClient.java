package no.nav.common.job.leader_election;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import no.nav.common.utils.EnvironmentUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Leader election implemented with ShedLock (https://github.com/lukas-krecan/ShedLock).
 * Guarantees that 0 or 1 leader is elected at any give moment. (Clock skew above 'CLOCK_SKEW_SECONDS' seconds might affect this claim)
 */
@Slf4j
public class ShedLockLeaderElectionClient implements LeaderElectionClient {

    private final static String LOCK_NAME = "leader-election";

    private final static Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(10);

    private final static Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(5);

    private final static int CLOCK_SKEW_SECONDS = 5;

    private final static int EXPIRATION_THRESHOLD_SECONDS = 120;

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
        if (hasShutDown) {
            return false;
        }

        if (hasAcquiredLock()) {
            extendLockIfAboutToExpire();
            return true;
        }

        return tryToAcquireLock();
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
                } else {
                    log.warn("Unable to extend lock");
                }
            } catch (UnsupportedOperationException uoe) {
                log.error("{} is being used with a lock provider that does not support lock extension", getClass().getCanonicalName());
            } catch (Exception e) {
                log.error("Caught exception when extending leader election lock", e);
            }
        }
    }

    private boolean tryToAcquireLock() {
        Instant createdAt = Instant.now();
        Optional<SimpleLock> maybeLock = lockProvider.lock(createLockConfig(createdAt));

        if (maybeLock.isPresent()) {
            setupNewLock(maybeLock.get(), createdAt);
            return true;
        }

        return false;
    }

    private void setupNewLock(SimpleLock newLock, Instant createdAt) {
        lock = newLock;
        lockExpiration = createdAt.plusMillis(LOCK_AT_MOST_FOR.toMillis());
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
        if (lockExpiration == null) {
            return false;
        }

        // Add clock skew to reduce the chance that more than 1 leader is elected simultaneously
        Instant currentTimeWithSkew = Instant.now().plusSeconds(CLOCK_SKEW_SECONDS);

        return currentTimeWithSkew.isBefore(lockExpiration);
    }

    private LockConfiguration createLockConfig(Instant createdAt) {
        return new LockConfiguration(createdAt, LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR);
    }

}
