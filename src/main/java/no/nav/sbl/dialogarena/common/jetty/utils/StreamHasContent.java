package no.nav.sbl.dialogarena.common.jetty.utils;

import org.apache.commons.collections15.Factory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Factory which returns <code>true</code> when an {@link InputStream} has any content that can be read. It is guarantied to not
 * throw IOException from the stream, e.g. if the stream has been closed the function simply returns false, not an exception.
 * Any no.nav.security.oidc.rest.resource management is the responsibility of the client of this class.
 */
public class StreamHasContent implements Factory<Boolean>, Serializable {

    private InputStream stream;

    public StreamHasContent(InputStream stream) {
        this.stream = stream;
    }

    @Override
    public Boolean create() {
        try {
            return stream.available() >= 1;
        } catch (IOException e) {
            return false;
        }
    }

}