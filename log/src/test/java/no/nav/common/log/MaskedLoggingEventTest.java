package no.nav.common.log;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskedLoggingEventTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaskedLoggingEvent.class);

    public static final String MASKED_FNR = "***********";

    @Test
    public void masked() {
        assertMasked("-12345678901");
        assertMasked("12345678901");
        assertMasked(" 12345678901");
        assertMasked("12345678901 ");
        assertMasked(" 12345678901 ");
        assertMasked("abc 12345678901 def");
        assertMasked("callId=7b7c<12345676543>c8c32129c837808f7");
    }

    @Test
    public void unmasked() {
        assertUnmasked("");
        assertUnmasked("abc");
        assertUnmasked("1234");
        assertUnmasked("1234567890");
        assertUnmasked("123456789012");
        assertUnmasked("callId=7b7c12345676543c8c32129c837808f7");
    }

    @Test
    public void nullValue() {
        assertThat(MaskedLoggingEvent.mask(null)).isNull();
    }

    @Test
    public void formatting() {
        assertMaskedAS("12345678901-12345678901 12345678901", MASKED_FNR + "-" + MASKED_FNR + " " + MASKED_FNR);
        assertMaskedAS("12345678901,12345678901", MASKED_FNR + "," + MASKED_FNR);
    }

    private void assertMasked(String string) {
        LOGGER.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).contains(MASKED_FNR);
    }

    private void assertUnmasked(String string) {
        LOGGER.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).doesNotContain("*");
    }

    private void assertMaskedAS(String string, String expected) {
        LOGGER.info(string);
        assertThat(MaskedLoggingEvent.mask(string)).isEqualTo(expected);
    }

}
