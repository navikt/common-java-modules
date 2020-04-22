package no.nav.common.utils;

import lombok.Builder;
import lombok.Value;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.*;
import static no.nav.common.utils.FunctionalUtils.combineOptional;
import static org.assertj.core.api.Assertions.assertThat;

public class FunctionalUtilsTest {

    @Test
    public void reducer_kan_kombinere_optionals() {
        Optional<Boolean> aBoolean = of(true);
        Optional<String> string = of("string");
        Optional<String> emptyString = empty();

        assertThat(of(TestObject.builder())
                .flatMap(combineOptional(aBoolean, TestObject.TestObjectBuilder::aBoolean))
                .flatMap(combineOptional(string, TestObject.TestObjectBuilder::string))
                .map(TestObject.TestObjectBuilder::build)
        ).hasValue(new TestObject(true, "string"));

        assertThat(of(TestObject.builder())
                .flatMap(combineOptional(aBoolean, TestObject.TestObjectBuilder::aBoolean))
                .flatMap(combineOptional(emptyString, TestObject.TestObjectBuilder::string))
        ).isEmpty();
    }

    @SuppressWarnings("unused")
    private void biConsumerMedCheckedException(String a, String b) throws IOException {
        throw new IOException();
    }

    private void consumerMedCheckedException(Object a) throws IOException {
        throw new IOException();
    }

    @SuppressWarnings("unused")
    private String functionMedCheckedException(String s) throws IOException {
        throw new IOException();
    }

    @SuppressWarnings("unused")
    private String supplierMedCheckedException() throws IOException {
        throw new IOException();
    }

    private <T> T alltidNull(T t) {
        return null;
    }

    @Value
    @Builder
    private static class TestObject {
        private boolean aBoolean;
        private String string;
    }

}