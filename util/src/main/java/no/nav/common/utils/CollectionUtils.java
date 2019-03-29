package no.nav.common.utils;

import java.util.*;

public class CollectionUtils {


    /**
     * Splitt opp liste i partisjoner av størrelse partitionSize. En liste med 10 elementer og partitionSize 2 vil da se
     * slik ut:
     *
     * [1,2,3,4,5,6,7,8,9] => [[0,1],[2,3],[4,5],[6,7],[8,9]]
     *
     * Merk at siste element vil kunne inneholde færre enn partitionSize elementer.
     *
     */
    public static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> partitionedList = new ArrayList<>();
        int listSize = list.size();

        for (int i = 0; i < listSize; i = i + partitionSize) {
            int toIndex = i + partitionSize;
            if (toIndex > listSize) {
                toIndex = listSize;
            }
            List<T> sublist = list.subList(i, toIndex);
            partitionedList.add(sublist);
        }
        return partitionedList;
    }

    public static <T> List<T> listOf(T... varargs) {
        return Arrays.asList(varargs);
    }

    public static <T> List<T> listOf(T singleton) {
        return Collections.singletonList(singleton);
    }

    public static <T> Set<T> setOf(T... varargs) {
        List<T> list = Arrays.asList(varargs);
        return new HashSet<>(list);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Pair<K,V>... varargs) {

        Map<K, V> map = new HashMap<>();
        for (Pair<K, V> vararg : varargs) {
            map.put(vararg.getFirst(), vararg.getSecond());
        }
        return map;
    }


    public static<T> List<T> toList(Set<T> set) {
        if (set == null) {
            return null;
        }
        return new ArrayList<>(set);
    }
}
