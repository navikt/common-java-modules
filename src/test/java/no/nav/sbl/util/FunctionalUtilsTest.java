package no.nav.sbl.util;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static no.nav.sbl.util.FunctionalUtils.sneaky;

public class FunctionalUtilsTest {

    @Test
    public void sneaky_(){
        new HashMap<String,String>().forEach(sneaky(this::funksjonMedCheckedException));
    }

    @SuppressWarnings("unused")
    private void funksjonMedCheckedException(String a, String b) throws IOException {
        throw new IOException();
    }

}