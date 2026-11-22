package no.nav.common.kafka.consumer;

/**
 * Felles grensesnitt for å starte, stoppe og lukke en Kafka-klient.
 *
 * <p>Klienten kan startes igjen etter {@link #stop()}. {@link #close()} brukes ved endelig
 * lukking, men standardmetoden kaller bare stop av hensyn til eksisterende implementasjoner.
 * Implementasjoner som eier ressurser, må overstyre close for å vente på arbeid som pågår,
 * frigjøre ressursene og avvise senere start.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html">Java 21 AutoCloseable</a>
 */
public interface KafkaConsumerClient extends AutoCloseable {

    /**
     * Starter konsumering eller gjenopptar en stoppet klient uten overlappende kjøringer.
     *
     * @throws IllegalStateException hvis implementasjonen har startet endelig lukking,
     *     eller ventingen på forrige kjøring feiler
     */
    void start();

    /**
     * Ber klienten stoppe slik at den kan startes igjen senere.
     *
     * <p>Implementasjonen bestemmer om metoden venter på aktivt arbeid. En stoppet klient
     * kan startes igjen. Bibliotekets implementasjoner logger tidsavbrudd og avbrutt venting
     * uten å kaste feil. Retur bekrefter derfor ikke at arbeidet er ferdig.
     * Bruk {@link KafkaConsumerClientImpl#stop(java.time.Duration)} for bekreftet stopp
     * av den konkrete consumer-klienten, eller close for endelig lukking.
     *
     * @throws IllegalStateException ved ulovlig selvventing eller feil i den stoppede kjøringen
     */
    void stop();

    /**
     * Lukker klienten og frigjør ressurser når implementasjonen støtter det.
     *
     * <p>Standardmetoden kaller bare {@link #stop()}. Den hindrer ikke senere start
     * og frigjør ikke nødvendigvis executor-ressurser i tredjepartsimplementasjoner.
     *
     * @throws IllegalStateException hvis stop eller implementasjonens lukking feiler
     * @see #stop()
     */
    @Override
    default void close() {
        stop();
    }

    /**
     * Leser om konsumering er aktivert.
     *
     * @return true når klienten er satt til å kjøre; false betyr ikke nødvendigvis at arbeidet er ferdig
     */
    boolean isRunning();
}
