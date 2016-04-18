package no.nav.sbl.dialogarena.common.jetty;

import no.nav.modig.lang.collections.combine.ChainedRunnables;
import no.nav.modig.lang.collections.factory.StreamHasContent;
import no.nav.modig.lang.util.WaitFor;
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

    public static WaitFor waitFor(Factory<Boolean> toReturnTrue) {
        return new WaitFor(toReturnTrue);
    }

}
