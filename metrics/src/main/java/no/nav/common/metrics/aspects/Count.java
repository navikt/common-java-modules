package no.nav.common.metrics.aspects;

import java.lang.annotation.*;

/**
 * Se {@link CountAspect} for eksempler på bruk.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Count {
    /**
     * Navnet på eventet genereres automatisk, men kan overskrives her.
     * Om name settes på Count på en klasse, vil et punktum og metodenavnet bli lagt
     * på i tillegg.
     */
    String name() default "";

    /**
     * Brukes bare om annotasjonen er på en klasse.
     * I tilleg blir hashCode, equals og toString alltid ignorert.
     * <p>
     * Finnes ikke en tilsvarende "inkluderteMetoder" som i proxy-versjonen,
     * sett i stedet da annotasjonen direkte kun på de metodene som ønskes
     */
    String[] ignoredMethods() default {};

    /**
     * Om argumenter til metoden skal logges sammen med eventet.
     * Kan kun brukes når annotasjonen brukes rett på en metode.
     */
    Field[] fields() default {};
}

