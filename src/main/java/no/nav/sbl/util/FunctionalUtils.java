package no.nav.sbl.util;

import no.nav.sbl.util.fn.UnsafeBiConsumer;

import java.util.function.BiConsumer;

public class FunctionalUtils {

    public static <T, U> BiConsumer<T, U> sneaky(UnsafeBiConsumer<T, U> unsafeBiConsumer){
        return unsafeBiConsumer;
    }

}
