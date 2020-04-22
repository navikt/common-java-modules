package no.nav.common.jetty;

import no.nav.common.jetty.utils.ChainedRunnables;
import no.nav.common.jetty.utils.Pause;
import no.nav.common.jetty.utils.StreamHasContent;
import org.apache.commons.collections15.Factory;

import java.io.InputStream;

public class JettyStarterUtils {

    public static Factory<Boolean> gotKeypress() {
        return streamHasContent(System.in);
    }

    public static Factory<Boolean> streamHasContent(InputStream stream) {
        return new StreamHasContent(stream);
    }

    public static ChainedRunnables first(Runnable task) {
        return new ChainedRunnables(task);
    }

    public static Runnable waitFor(Factory<Boolean> toReturnTrue) {
        return () -> Pause.pause().pollEveryMs(400).noTimeout().until(toReturnTrue);
    }

}
