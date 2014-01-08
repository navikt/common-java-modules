package no.nav.sbl.dialogarena.test.match;

import org.apache.commons.collections15.Predicate;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class PredicateAsMatcher<T> extends TypeSafeMatcher<T> {

    private final String description;

    private final Predicate<T> predicate;


    PredicateAsMatcher(Predicate<T> predicate) {
        this(predicate.getClass().getSimpleName(), predicate);
    }

    public PredicateAsMatcher(String description, Predicate<T> predicate) {
        this.description = description;
        this.predicate = predicate;
    }


    @Override
    public void describeTo(Description desc) {
        desc.appendText(this.description);
    }


    @Override
    protected boolean matchesSafely(T item) {
        return predicate.evaluate(item);
    }

}