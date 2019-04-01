package no.nav.common.utils;

import lombok.Value;

@Value(staticConstructor = "of")
public class Pair<K, V> {
    K first;
    V second;
}
