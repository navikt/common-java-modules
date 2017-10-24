package no.nav.sbl.util;

import lombok.Builder;
import lombok.Value;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.*;
import static java.util.Optional.ofNullable;
import static no.nav.sbl.util.FunctionalUtils.combineOptional;
import static no.nav.sbl.util.FunctionalUtils.sneaky;
import static no.nav.sbl.util.FunctionalUtils.sneakyFunction;
import static org.assertj.core.api.Assertions.assertThat;

public class FunctionalUtilsTest {

    @Test
    public void sneaky_lambdaer_og_metodereferanser_kompilerer_utvetydig() {
        new HashMap<String, String>().forEach(sneaky(this::biConsumerMedCheckedException));
        new HashMap<String, String>().forEach(sneaky((k, v) -> biConsumerMedCheckedException(k, v)));

        asList().forEach(sneaky(this::consumerMedCheckedException));
        asList().forEach(sneaky((k) -> consumerMedCheckedException(k)));

        ofNullable("").orElseGet(sneaky(this::supplierMedCheckedException));
        ofNullable("").orElseGet(sneaky(() -> supplierMedCheckedException()));

        ofNullable("").map(this::alltidNull).map(sneakyFunction((s) -> functionMedCheckedException(s)));
        ofNullable("").map(this::alltidNull).map(sneakyFunction(this::functionMedCheckedException));
    }

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