package no.nav.fo.feed.consumer;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import static java.lang.Thread.sleep;
import static no.nav.fo.feed.consumer.FeedPoller.createScheduledJob;
import static no.nav.fo.feed.consumer.FeedPoller.shutdown;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;


public class FeedPollerTest {

    private static final Logger LOG = getLogger(FeedPollerTest.class);
    private static final String EN_GANG_PER_SEKUND = "/1 * * * * ?";

    @After
    public void cleanup(){
        shutdown();
    }

    @Test
    public void createScheduledJob_ingenParallellKjoring() throws InterruptedException {
        int behandlingstidForFeed = 3000;
        Runnable treigFeed = mockFeed(behandlingstidForFeed);
        createScheduledJob("test", "test", EN_GANG_PER_SEKUND, treigFeed);
        sleep((long) (behandlingstidForFeed * 2.5));
        verify(treigFeed, times(3)).run();
    }

    @Test
    public void shutdown_kanResetteScheduler() throws InterruptedException {
        createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
        shutdown();
        createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
        shutdown();
        createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
    }

    private Runnable mockFeed(int millis) {
        Runnable runnable = mock(Runnable.class);
        Answer<Object> treigFeed = (InvocationOnMock invocationOnMock) -> {
            LOG.info("start");
            sleep(millis);
            LOG.info("stopp");
            return null;
        };
        doAnswer(treigFeed).when(runnable).run();
        return runnable;
    }

}