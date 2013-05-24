package no.nav.sbl.dialogarena.types;

/**
 * En type som kan gi en kopiert instans av seg selv.
 *
 * @param <T> Typen det kan lages kopier av, skal som regel types til
 *            samme klasse som implementerer interfaces.
 */
public interface Copyable<T> {
    T copy();
}
