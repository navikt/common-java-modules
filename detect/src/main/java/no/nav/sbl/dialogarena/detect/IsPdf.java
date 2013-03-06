package no.nav.sbl.dialogarena.detect;

import org.apache.commons.collections15.Predicate;
import org.apache.tika.Tika;

public class IsPdf implements Predicate<byte[]> {
    @Override
    public boolean evaluate(byte[] bytes) {
        return new Tika().detect(bytes).equalsIgnoreCase("application/pdf");
    }
}
