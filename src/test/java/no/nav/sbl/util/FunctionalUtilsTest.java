package no.nav.sbl.util;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static no.nav.sbl.util.FunctionalUtils.sneaky;

public class FunctionalUtilsTest {

    @Test
    public void sneaky_() {
        new HashMap<String, String>().forEach(sneaky(this::biConsumerMedCheckedException));
        new HashMap<String, String>().forEach(sneaky((k, v) -> System.out.println(v)));
        asList().forEach(sneaky(this::consumerMedCheckedException));
        asList().forEach(sneaky((k) -> System.out.println(k)));
        ofNullable("not null").orElseGet(sneaky(this::supplierMedCheckedException));
        ofNullable("not null").orElseGet(sneaky(() -> "test"));
    }

    @SuppressWarnings("unused")
    private void biConsumerMedCheckedException(String a, String b) throws IOException {
        throw new IOException();
    }

    private void consumerMedCheckedException(Object a) throws IOException {
        throw new IOException();
    }

    @SuppressWarnings("unused")
    private String supplierMedCheckedException() throws IOException {
        throw new IOException();
    }

}