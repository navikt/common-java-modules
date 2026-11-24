package no.nav.common.utils;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.nav.common.utils.FunctionalUtils.combineOptional;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;

public class FunctionalUtilsTest {

    @Test
    public void reducer_kan_kombinere_optionals() {
        Optional<Boolean> aBoolean = of(true);
        Optional<String> string = of("string");
        Optional<String> emptyString = empty();

        assertThat(of(TestObject.builder())
                        .flatMap(combineOptional(aBoolean, TestObject.TestObjectBuilder::aBoolean))
                        .flatMap(combineOptional(string, TestObject.TestObjectBuilder::string))
                        .map(TestObject.TestObjectBuilder::build))
                .hasValue(new TestObject(true, "string"));

        assertThat(of(TestObject.builder())
                        .flatMap(combineOptional(aBoolean, TestObject.TestObjectBuilder::aBoolean))
                        .flatMap(combineOptional(emptyString, TestObject.TestObjectBuilder::string)))
                .isEmpty();
    }

    @Value
    @Builder
    private static class TestObject {
        boolean aBoolean;
        String string;
    }
}
