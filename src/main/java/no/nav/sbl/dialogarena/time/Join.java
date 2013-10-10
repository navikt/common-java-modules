package no.nav.sbl.dialogarena.time;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections15.FactoryUtils.constantFactory;
import static org.apache.commons.lang3.StringUtils.join;

public class Join<T> implements Transformer<T, String> {
    private final Transformer<? super T,String>[] transformers;
    private final Factory<String> separator;

    @SafeVarargs
    public Join(String separator, Transformer<? super T, String> ... transformers) {
        this.transformers = transformers;
        this.separator = constantFactory(separator);
    }

    @SafeVarargs
    public Join(Factory<String> separator, Transformer<? super T, String> ... transformers) {
        this.transformers = transformers;
        this.separator = separator;
    }

    @Override
    public String transform(T input) {
        List<String> strings = new ArrayList<>();
        for (Transformer<? super T, String> transformer : transformers) {
            strings.add(transformer.transform(input));
        }
        return join(strings, separator.create());
    }
}