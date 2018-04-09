package no.nav.fo.feed.util;

import no.nav.fo.feed.exception.InvalidUrlException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.fo.feed.util.UrlValidator.isInvalidUrl;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UrlValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldNotBeValid() {
        assertThat(isInvalidUrl("htp://veilarb.com")).isTrue();
    }

    @Test
    public void shouldBeValid() {
        assertThat(UrlValidator.isValidUrl("https://veilarb.com")).isTrue();
    }

    @Test
    public void shouldInvalidateMalformedUrl() throws Exception {
        exception.expect(InvalidUrlException.class);
        UrlValidator.validateUrl("htps://");
    }
}