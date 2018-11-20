package no.nav.metrics.aspects;

import java.lang.annotation.*;

/**
 * @deprecated don't use aspects for metrics, just measure directly using MetricsFactory.getMeterRegistry()
 */
@Deprecated
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {
    /**
     * Navnet på timeren genereres automatisk, men kan overskrives her.
     * Om name settes på Timed på en klasse, vil et punktum og metodenavnet bli lagt
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
}
