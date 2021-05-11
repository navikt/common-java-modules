package no.nav.common.job.leader_election;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ShedLockLeaderElectionClientTest {

    @Test
    public void should_acquire_lock_for_one_client_and_deny_the_other() {
       AtomicInteger counter = new AtomicInteger();

        LockProvider provider = lockConfiguration -> {
            if (counter.incrementAndGet() > 1) {
                return Optional.empty();
            } else {
                return Optional.of(() -> {});
            }
        };

        ShedLockLeaderElectionClient client1 = new ShedLockLeaderElectionClient(provider);
        ShedLockLeaderElectionClient client2 = new ShedLockLeaderElectionClient(provider);

        assertTrue(client1.isLeader());
        assertFalse(client2.isLeader());

        assertTrue(client1.isLeader());
        assertFalse(client2.isLeader());

        assertEquals(3, counter.get()); // Client 1 should only increment once
    }

}
