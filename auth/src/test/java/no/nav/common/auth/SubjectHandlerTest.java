package no.nav.common.auth;

import no.nav.brukerdialog.security.domain.IdentType;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SubjectHandlerTest {

    @Test
    public void withSubjectProvider() {
        SubjectHandler.withSubjectProvider(() -> null, this::assertNoSubject);
    }

    @Test
    public void withSubject__no_leakage_to_otherThreads_or_contexts() {
        Subject subject = newSubject("uid");
        assertThat(SubjectHandler.getSubject()).isEmpty();
        SubjectHandler.withSubject(subject, () -> {
            assertThat(SubjectHandler.getSubject()).hasValue(subject);

            // fork-join thread pool
            Set<Thread> usedThreads = new HashSet<>();
            Thread baseThread = Thread.currentThread();
            IntStream.range(0, 50)
                    .parallel()
                    .forEach((i) -> {
                        try {
                            // prevent reuse of main thread
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Thread thread = Thread.currentThread();
                        usedThreads.add(thread);
                        if (thread == baseThread) {
                            assertThat(SubjectHandler.getSubject()).hasValue(subject);
                        } else {
                            assertNoSubject();
                        }
                    });
            assertThat(usedThreads.size()).isGreaterThan(1);

            // child thread
            AtomicReference<Subject> subjectWrapper = new AtomicReference<>();
            Thread thread = new Thread(() -> SubjectHandler.getSubject().ifPresent(subjectWrapper::set));
            thread.start();
            thread.join();
            assertThat(subjectWrapper.get()).isNull();

            // empty child context
            SubjectHandler.withSubject(null, this::assertNoSubject);

            // populated child context
            Subject otherSubject = newSubject("other");
            SubjectHandler.withSubject(otherSubject, () -> {
                assertThat(SubjectHandler.getSubject()).hasValue(otherSubject);
            });

            // failing child context
            assertThatThrownBy(() -> {
                SubjectHandler.withSubject(otherSubject, () -> {
                    throw new Error();
                });
            }).isInstanceOf(Error.class);

            assertThat(SubjectHandler.getSubject()).hasValue(subject);
        });
        assertThat(SubjectHandler.getSubject()).isEmpty();
    }

    private Subject newSubject(String uid) {
        return TestSubjectUtils.builder().uid(uid).build();
    }

    private void assertNoSubject() {
        assertThat(SubjectHandler.getSubject())
                .describedAs("subject on thread " + Thread.currentThread().getName())
                .isEmpty();
    }

}