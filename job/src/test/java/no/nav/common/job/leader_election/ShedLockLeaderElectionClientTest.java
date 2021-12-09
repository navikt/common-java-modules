package no.nav.common.job.leader_election;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ShedLockLeaderElectionClientTest {

    @Test
    public void should_acquire_lock_for_one_client_and_deny_the_other() throws InterruptedException {
       AtomicInteger counter = new AtomicInteger();

        LockProvider provider = lockConfiguration -> {
            if (counter.incrementAndGet() > 1) {
                return Optional.empty();
            } else {
                return Optional.of(() -> {});
            }
        };

        ShedLockLeaderElectionClient client1 = new ShedLockLeaderElectionClient(provider);
        Thread.sleep(100); // Ensures that client1 is always the leader

        ShedLockLeaderElectionClient client2 = new ShedLockLeaderElectionClient(provider);
        Thread.sleep(100); // Ensures that client2 has tried to check the lock once

        assertTrue(client1.isLeader());
        assertFalse(client2.isLeader());

        assertTrue(client1.isLeader());
        assertFalse(client2.isLeader());

        assertEquals(2, counter.get()); // Clients should only increment once
    }

}
