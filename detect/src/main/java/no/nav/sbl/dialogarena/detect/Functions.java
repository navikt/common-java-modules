package no.nav.sbl.dialogarena.detect;

import org.apache.commons.collections15.Transformer;
import org.apache.tika.Tika;

import static org.apache.commons.lang3.StringUtils.lastIndexOf;
import static org.apache.commons.lang3.StringUtils.substring;

public final class Functions {

    private Functions() {
    }

    public static final Transformer<byte[], String> CONTENT_TYPE = new Transformer<byte[], String>() {
        private final Tika tika = new Tika();

        @Override
        public String transform(byte[] data) {
            return tika.detect(data);
        }
    };

    public static final Transformer<String, String> CONTENT_SUBTYPE = new Transformer<String, String>() {
        @Override
        public String transform(String contentType) {
            return substring(contentType, lastIndexOf(contentType, "/") + 1);
        }
    };
}