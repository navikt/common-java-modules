package no.nav.fo.feed;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class TestLockProvider implements LockProvider {
    public static int locksGiven = 0;
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        if (lock.tryLock()) {
            locksGiven++;
            return Optional.of(new MySimpleLock(lock));
        }
        return Optional.empty();
    }

    static class MySimpleLock implements SimpleLock {

        private final ReentrantLock lock;

        public MySimpleLock(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void unlock() {
            lock.unlock();
        }
    }
}
