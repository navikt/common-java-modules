package no.nav.sbl.dialogarena.detect;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tika.Tika;

public class IsPng implements Predicate<byte[]> {
    public boolean evaluate(byte[] bytes) {
        final int endIndexExclusive = 2048;
        return new Tika().detect(ArrayUtils.subarray(bytes.clone(), 0, endIndexExclusive)).equalsIgnoreCase("image/png");
    }
}
