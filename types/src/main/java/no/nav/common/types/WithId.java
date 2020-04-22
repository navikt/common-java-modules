package no.nav.common.types;


/**
 * En type som er identifiserbar med en id.
 *
 * @param <T> typen til id-en f.eks. {@link Long} eller {@link String}.
 */
public interface WithId<T> {
    T getId();
}
