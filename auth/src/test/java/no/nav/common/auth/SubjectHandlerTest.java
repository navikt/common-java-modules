package no.nav.common.auth;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SubjectHandlerTest {

    @Test
    public void withSubjectProvider() {
        SubjectHandler.withSubjectProvider(() -> null, () -> {
            assertThat(SubjectHandler.getSubject()).isEmpty();
        });
    }

}