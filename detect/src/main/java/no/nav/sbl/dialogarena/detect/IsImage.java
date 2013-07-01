package no.nav.sbl.dialogarena.detect;

import org.apache.commons.collections15.Predicate;

public class IsImage implements Predicate<byte[]> {

    @Override
    public final boolean evaluate(byte[] bytes) {
        return new IsPng().evaluate(bytes) || new IsJpg().evaluate(bytes);
    }
}
