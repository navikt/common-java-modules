package no.nav.util.sbl;

import java.util.Arrays;
import java.util.Optional;

public class EnumUtils {

    public static <T extends Enum> Optional<T> valueOf(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny();
    }

}
