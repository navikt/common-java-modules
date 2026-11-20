package no.nav.common.test.match;

import java.util.function.Predicate;
import org.hamcrest.Matcher;

public final class Matchers {

    private Matchers() {}

    public static <T> Matcher<T> match(Predicate<T> predicate) {
        return new PredicateAsMatcher<>(predicate);
    }

    public static <T> Matcher<T> match(String description, Predicate<T> predicate) {
        return new PredicateAsMatcher<>(description, predicate);
    }
}
