package no.nav.fo.feed.util;

import no.nav.fo.feed.exception.InvalidUrlException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.fo.feed.util.UrlValidator.isInvalidUrl;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UrlValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldNotBeValid() {
        assertTrue(isInvalidUrl("htp://veilarb.com"));
    }

    @Test
    public void shouldBeValid() {
        assertTrue(UrlValidator.isValidUrl("https://veilarb.com"));
    }

    @Test
    public void shouldInvalidateMalformedUrl() throws Exception {
        exception.expect(InvalidUrlException.class);
        UrlValidator.validateUrl("htps://");
    }
}