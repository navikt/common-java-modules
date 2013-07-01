package no.nav.sbl.dialogarena.detect;

import org.apache.commons.collections15.Predicate;
import org.apache.tika.Tika;

public class IsJpg implements Predicate<byte[]> {
    @Override
    public final boolean evaluate(byte[] bytes) {
        return new Tika().detect(bytes).equalsIgnoreCase("image/jpeg");
    }
}