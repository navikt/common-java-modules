package no.nav.sbl.dialogarena.types;

import no.nav.sbl.dialogarena.types.Pingable.Ping;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

/**
 * Operasjoner som tilbys av ulike typer, wrappet i funksjonsobjekter.
 */
public final class Get {

    public static <T> Transformer<Copyable<T>, T> copy() {
        return copyable -> copyable.copy();
    }

    public static <T> Transformer<WithId<T>, T> id() {
        return withId -> withId.getId();
    }

    public static Transformer<WithKodeverkId, String> kodeverkId() {
        return withKodeverkId -> withKodeverkId.getKodeverkId();
    }

    public static <T> Predicate<WithId<T>> medId(final T id) {
        return object -> id.equals(object.getId());
    }

    public static Transformer<Pingable, Ping> pingResult() {
        return pingable -> {
            long start = System.currentTimeMillis();
            Ping ping = pingable.ping();
            ping.setResponstid(System.currentTimeMillis() - start);
            return ping;
        };
    }

    public static Predicate<Ping> vellykketPing() {
        return ping -> ping.erVellykket();
    }

    private Get() { }
}
