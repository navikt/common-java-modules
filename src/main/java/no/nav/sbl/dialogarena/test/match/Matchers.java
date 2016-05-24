package no.nav.sbl.dialogarena.test.match;

import org.hamcrest.Matcher;

import java.util.function.Predicate;

public final class Matchers {

    private Matchers() {
    }

    public static <T> Matcher<T> match(Predicate<T> predicate) {
        return new PredicateAsMatcher<>(predicate);
    }

    public static <T> Matcher<T> match(String description, Predicate<T> predicate) {
        return new PredicateAsMatcher<>(description, predicate);
    }
}