package no.nav.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MaskedLoggingEventTest {
    public static final String MASKED_FNR = "***********";

    @Test
    public void masked() {
        // Arrange
        String[] inputs = {
            "-12345678901",
            "12345678901",
            " 12345678901",
            "12345678901 ",
            " 12345678901 ",
            "abc 12345678901 def",
            "callId=7b7c<12345676543>c8c32129c837808f7"
        };

        // Act & Assert
        for (String input : inputs) {
            assertMasked(input);
        }
    }

    @Test
    public void unmasked() {
        // Arrange
        String[] inputs = {"", "abc", "1234", "1234567890", "123456789012", "callId=7b7c12345676543c8c32129c837808f7"};

        // Act & Assert
        for (String input : inputs) {
            assertUnmasked(input);
        }
    }

    @Test
    public void nullValue() {
        // Arrange
        String input = null;

        // Act
        // Null er tilsiktet input i denne kontrakttesten.
        //noinspection ConstantValue
        String masked = MaskedLoggingEvent.mask(input);

        // Assert
        //noinspection ConstantValue
        assertThat(masked).isNull();
    }

    @Test
    public void formatting() {
        // Arrange
        String separatedInput = "12345678901-12345678901 12345678901";
        String commaSeparatedInput = "12345678901,12345678901";

        // Act & Assert
        assertMaskedAS(separatedInput, MASKED_FNR + "-" + MASKED_FNR + " " + MASKED_FNR);
        assertMaskedAS(commaSeparatedInput, MASKED_FNR + "," + MASKED_FNR);
    }

    @Test
    @SuppressWarnings("deprecation") // Tester Logbacks beholdte MDC-alias.
    public void gammelt_mdc_alias_beholder_maskeringen() {
        // Arrange
        var event = new LoggingEvent();
        event.setMDCPropertyMap(Map.of("ident", "12345678901", "requestId", "request-1"));
        var maskedEvent = new MaskedLoggingEvent(event);

        // Act
        Map<String, String> mdc = maskedEvent.getMdc();

        // Assert
        assertThat(mdc)
                .isEqualTo(maskedEvent.getMDCPropertyMap())
                .containsEntry("ident", MASKED_FNR)
                .containsEntry("requestId", "request-1");
    }

    private void assertMasked(String string) {
        log.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).contains(MASKED_FNR);
    }

    private void assertUnmasked(String string) {
        log.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).doesNotContain("*");
    }

    private void assertMaskedAS(String string, String expected) {
        log.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).isEqualTo(expected);
    }
}
