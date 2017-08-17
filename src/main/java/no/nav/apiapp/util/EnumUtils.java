package no.nav.apiapp.util;

import java.util.Arrays;
import java.util.Optional;

public class EnumUtils {

    public static String getName(Enum<?> anEnum){
        return anEnum != null ? anEnum.name() : null;
    }

    public static <T extends Enum> Optional<T> valueOfOptional(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny();
    }

}
