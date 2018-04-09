package no.nav.sbl.dialogarena.common.jetty;

import no.nav.sbl.dialogarena.common.jetty.utils.ChainedRunnables;
import no.nav.sbl.dialogarena.common.jetty.utils.StreamHasContent;
import org.apache.commons.collections15.Factory;

import java.io.InputStream;

import static no.nav.sbl.dialogarena.common.jetty.utils.Pause.pause;

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
        return () -> pause().pollEveryMs(400).noTimeout().until(toReturnTrue);
    }

}
