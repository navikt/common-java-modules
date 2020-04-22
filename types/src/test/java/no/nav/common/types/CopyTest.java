package no.nav.common.types;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;


public class CopyTest {

    class X implements Copyable<X> {
        int a;

        @Override
        public X copy() {
            return new X() {{ a = X.this.a; }};
        }
    }

    @Test
    public void copiesAnObject() {

        X original = new X() {{ a = 42; }};
        X copy = Get.<X>copy().transform(original);

        assertThat(original, not(sameInstance(copy)));
        assertThat(original.a, is(copy.a));
    }
}
