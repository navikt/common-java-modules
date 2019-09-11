package no.nav.util.common;

public final class Pair<K, V> {
    private final K first;
    private final V second;

    private Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public static <K, V> Pair<K, V> of(K first, V second) {
        return new Pair<K, V>(first, second);
    }

    public K getFirst() {
        return this.first;
    }

    public V getSecond() {
        return this.second;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Pair)) return false;
        final Pair<?, ?> other = (Pair<?, ?>) o;
        final Object this$first = this.getFirst();
        final Object other$first = other.getFirst();
        if (this$first == null ? other$first != null : !this$first.equals(other$first)) return false;
        final Object this$second = this.getSecond();
        final Object other$second = other.getSecond();
        if (this$second == null ? other$second != null : !this$second.equals(other$second)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $first = this.getFirst();
        result = result * PRIME + ($first == null ? 43 : $first.hashCode());
        final Object $second = this.getSecond();
        result = result * PRIME + ($second == null ? 43 : $second.hashCode());
        return result;
    }

    public String toString() {
        return "Pair(first=" + this.getFirst() + ", second=" + this.getSecond() + ")";
    }
}
