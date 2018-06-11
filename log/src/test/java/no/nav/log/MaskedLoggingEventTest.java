package no.nav.log;


import no.nav.sbl.util.LogUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskedLoggingEventTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    public static final String MASKED_FNR = "***********";

    @Test
    public void masked() {
        assertMasked("-12345678901");
        assertMasked("12345678901");
        assertMasked(" 12345678901");
        assertMasked("12345678901 ");
        assertMasked(" 12345678901 ");
        assertMasked("abc 12345678901 def");
        assertMasked("abc12345678901def");
        assertMasked("abc12345678901def");
    }

    @Test
    public void unmasked() {
        assertUnmasked("abc");
        assertUnmasked("1234");
        assertUnmasked("1234567890");
        assertUnmasked("123456789012");
    }

    @Test
    public void formatting() {
        assertMaskedAS("abc12345678901def", "abc" + MASKED_FNR + "def");
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
