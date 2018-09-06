package no.nav.log;

import org.junit.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static no.nav.log.MDCUtils.withContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MDCUtilsTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Test
    public void withContext__context_available_in_callback() {
        Map<String, String> context = new HashMap<String, String>() {{
            put(KEY, VALUE);
        }};

        withContext(context, () -> {
            assertThat(MDC.get(KEY)).isEqualTo(VALUE);
        });

        assertThat(MDC.get(KEY)).isNull();
    }


    @Test
    public void withContext__original_context_restored() {
        MDC.put(KEY, VALUE);

        withContext(null, () -> {
            assertThat(MDC.get(KEY)).isNull();
        });

        assertThat(MDC.get(KEY)).isEqualTo(VALUE);
    }

    @Test
    public void withContext__original_context_restored_despite_exception() {
        MDC.put(KEY, VALUE);

        assertThatThrownBy(() -> {
            withContext(null, () -> {
                throw new IllegalStateException();
            });
        }).isExactlyInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(KEY)).isEqualTo(VALUE);
    }

}