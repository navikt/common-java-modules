package no.nav.common.job;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class JobRunnerTest {

    private ListAppender<ILoggingEvent> jobAppender;
    private Logger jobLogger = (Logger) LoggerFactory.getLogger(JobRunner.class);

    @Before
    public void setUp() {
        jobAppender = new ListAppender<>();
        jobAppender.start();
        jobLogger.addAppender(jobAppender);
    }

    @After
    public void tearDown() {
        jobLogger.detachAppender(jobAppender);
    }

    @Test
    public void should_log_job_start_and_end() {
        JobRunner.run("test", "id", () -> {});
        assertEquals(2, jobAppender.list.size());
        assertEquals("Job started. jobName=test jobId=id", jobAppender.list.get(0).getFormattedMessage());
        assertEquals("Job finished. jobName=test jobId=id", jobAppender.list.get(1).getFormattedMessage());
    }

    @Test
    public void should_log_job_start_and_failure() {
        try {
            JobRunner.run("test", "id", () -> {
                throw new RuntimeException();
            });
        } catch (Exception e) {}

        assertEquals(2, jobAppender.list.size());
        assertEquals("Job started. jobName=test jobId=id", jobAppender.list.get(0).getFormattedMessage());
        assertEquals("Job failed. jobName=test jobId=id", jobAppender.list.get(1).getMessage());
    }

    @Test
    public void should_add_mdc_properties() {
        JobRunner.run("test", "id", () -> {
            Logger testLogger = (Logger) LoggerFactory.getLogger(JobRunnerTest.class);
            ListAppender<ILoggingEvent> testAppender = new ListAppender<>();

            testAppender.start();
            testLogger.addAppender(testAppender);

            testLogger.info("test");

            assertEquals("{jobName=test, jobId=id}", testAppender.list.get(0).getMDCPropertyMap().toString());
        });
    }

}
