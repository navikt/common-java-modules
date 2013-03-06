package no.nav.sbl.dialogarena.test.match;

import org.apache.commons.collections15.Predicate;
import org.hamcrest.Matcher;

public class Matchers {

    public static <T> Matcher<T> match(Predicate<T> predicate) {
        return new PredicateAsMatcher<>(predicate);
    }

    public static <T> Matcher<T> match(String description, Predicate<T> predicate) {
        return new PredicateAsMatcher<>(description, predicate);
    }


}
