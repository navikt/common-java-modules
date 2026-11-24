package no.nav.common.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Slf4j
public class JobRunnerTest {
    private ListAppender<ILoggingEvent> jobAppender;
    private Map<String, String> originalContextMap;

    // Testen skal fange loggmeldinger fra produksjonsklassen.
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private final Logger jobLogger = (Logger) LoggerFactory.getLogger(JobRunner.class);

    @BeforeEach
    public void setUp() {
        originalContextMap = MDC.getCopyOfContextMap();
        MDC.clear();
        jobAppender = new ListAppender<>();
        jobAppender.start();
        jobLogger.addAppender(jobAppender);
    }

    @AfterEach
    public void tearDown() {
        try {
            if (jobAppender != null) {
                jobLogger.detachAppender(jobAppender);
                jobAppender.stop();
            }
        } finally {
            if (originalContextMap == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(originalContextMap);
            }
        }
    }

    @Test
    public void should_log_job_start_and_end() {
        JobRunner.run("test", "id", () -> {});
        assertEquals(2, jobAppender.list.size());
        assertEquals(
                "Job started. jobName=test jobId=id", jobAppender.list.get(0).getFormattedMessage());
        assertEquals(
                "Job finished. jobName=test jobId=id", jobAppender.list.get(1).getFormattedMessage());
    }

    @Test
    public void should_log_job_start_and_failure() {
        RuntimeException expectedException = new RuntimeException();
        RuntimeException actualException = assertThrows(
                RuntimeException.class,
                () -> JobRunner.run("test", "id", () -> {
                    throw expectedException;
                }));

        assertSame(expectedException, actualException);

        assertEquals(2, jobAppender.list.size());
        assertEquals(
                "Job started. jobName=test jobId=id", jobAppender.list.get(0).getFormattedMessage());
        assertEquals(
                "Job failed. jobName=test jobId=id", jobAppender.list.get(1).getMessage());
    }

    @Test
    public void should_add_mdc_properties() {
        JobRunner.run("test", "id", () -> {
            Logger testLogger = (Logger) log;
            ListAppender<ILoggingEvent> testAppender = new ListAppender<>();

            testAppender.start();
            testLogger.addAppender(testAppender);

            try {
                testLogger.info("test");

                assertEquals(
                        Map.of("jobName", "test", "jobId", "id"),
                        testAppender.list.getFirst().getMDCPropertyMap());
            } finally {
                testLogger.detachAppender(testAppender);
                testAppender.stop();
            }
        });
    }
}
