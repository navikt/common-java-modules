package no.nav.common.feed.consumer;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;


public class FeedPollerTest {

    private static final Logger LOG = getLogger(FeedPollerTest.class);
    private static final FeedConsumerConfig.ScheduleCreator EN_GANG_PER_SEKUND = new FeedConsumerConfig.CronPollingConfig("/1 * * * * ?");

    @After
    public void cleanup() {
        FeedPoller.shutdown();
    }

    @Test
    public void createScheduledJob_ingenParallellKjoring() throws InterruptedException {
        int behandlingstidForFeed = 3000;
        Runnable treigFeed = mockFeed(behandlingstidForFeed);
        FeedPoller.createScheduledJob("test", "test", EN_GANG_PER_SEKUND, treigFeed);
        sleep((long) (behandlingstidForFeed * 2.5));
        verify(treigFeed, times(3)).run();
    }

    @Test
    public void shutdown_kanResetteScheduler() throws InterruptedException {
        FeedPoller.createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
        FeedPoller.shutdown();
        FeedPoller.createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
        FeedPoller.shutdown();
        FeedPoller.createScheduledJob("test", "test", EN_GANG_PER_SEKUND, mock(Runnable.class));
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