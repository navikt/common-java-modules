package no.nav.sbl.dialogarena.test.match;

import org.junit.Test;

import static no.nav.modig.lang.collections.PredicateUtils.containsString;
import static no.nav.sbl.dialogarena.test.match.Matchers.match;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;


public class MathersTest {

    @Test
    public void adaptPredicateAsMatcher() {
        assertThat("ab", match(containsString("b")));
        assertFalse(match(containsString("b")).matches("a"));
    }
}
