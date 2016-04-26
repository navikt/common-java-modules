package no.nav.sbl.dialogarena.time;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.join;

public class Join<T> implements Function<T, String> {
    private final Function<? super T,String>[] transformers;
    private final String separator;

    //TODO her må det gjøres noe.
    public Join(String separator, Function<? super T, String> ... transformers) {
        this.transformers = transformers;
        this.separator = separator;
    }

    @Override
    public String apply(T input) {
        List<String> strings = new ArrayList<>();
        for (Function<? super T, String> transformer : transformers) {
            strings.add(transformer.apply(input));
        }
        return join(strings, separator);
    }
}