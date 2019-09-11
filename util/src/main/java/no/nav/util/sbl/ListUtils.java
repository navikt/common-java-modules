package no.nav.util.sbl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {

    public static <T> List<T> mutableList(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

}
