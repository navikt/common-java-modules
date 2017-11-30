package no.nav.sbl.sql.value;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.function.Function;

public class FunctionValue<T> extends Value<Tuple2<Class, Function<T, Object>>> {
    public FunctionValue(Class type, Function<T, Object> paramValue) {
        super(Tuple.of(type, paramValue));
    }

    @Override
    public boolean hasPlaceholder() {
        return true;
    }

    @Override
    public String getValuePlaceholder() {
        return "?";
    }
}
