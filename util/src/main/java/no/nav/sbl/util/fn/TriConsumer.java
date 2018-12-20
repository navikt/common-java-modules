package no.nav.sbl.util.fn;

@FunctionalInterface
public interface TriConsumer<A1, A2, A3> {

    void accept(A1 argument1, A2 argument2, A3 argument3);

}
