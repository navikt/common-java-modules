package no.nav.sbl.dialogarena.types;

import java.io.Serializable;
import java.util.UUID;

/**
 * Implementeres av komponenter skal overvåkes av selftest
 */
@SuppressWarnings("unused")
public interface Pingable {

    /**
     * Denne metoden må implementeres, og brukes til å sjekke et om en avhengighet er oppe. Det er viktig
     * at man fanger opp eventuelle exceptions i koden, da uhåndterte exceptions vil føre til at selftest-siden
     * returnerer status 500.
     * @return En vellykket eller feilet ping-respons.
     */
    Ping ping();

    final class Ping implements Serializable {
        private PingMetadata metadata;
        private String feilmelding;
        private Throwable feil;
        private boolean erAvskrudd;
        private long responstid = -1;

        public PingMetadata getMetadata() {
            return metadata;
        }

        public Ping setMetadata(PingMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public String getFeilmelding() {
            return feilmelding;
        }

        public Throwable getFeil() {
            return feil;
        }

        public long getResponstid() {
            return responstid;
        }

        public boolean harFeil() {
            return feil != null || feilmelding != null;
        }

        public boolean erVellykket() {
            return !this.harFeil();
        }

        public Ping setErAvskrudd(boolean erAvskrudd) {
            this.erAvskrudd = erAvskrudd;
            return this;
        }

        public boolean erAvskrudd() {
            return this.erAvskrudd;
        }

        public Ping setResponstid(Long responstid) {
            this.responstid = responstid;
            return this;
        }

        Ping setResponstid(long responstid) {
            this.responstid = responstid;
            return this;
        }

        Ping setFeil(Throwable feil) {
            this.feil = feil;
            return this;
        }

        Ping setFeilmelding(String feilmelding) {
            this.feilmelding = feilmelding;
            return this;
        }

        private Ping(PingMetadata metadata) {
            this.setMetadata(metadata);
        }

        /**
         * @param metadata Metadata om den pingbare-ressursen. Inneholder endepunkt, beskrivelse og om det er
         *                 en kritisk avhengighet eller ikke.
         * @return Et vellykket pingresultat som kan bruks til generering av selftester.
         */
        public static Ping lyktes(PingMetadata metadata) {
            return new Ping(metadata);
        }

        /**
         * @param metadata Metadata om den pingbare-ressursen. Inneholder endepunkt, beskrivelse og om det er
         *                 en kritisk avhengighet eller ikke.
         * @return Et pingresultat som kan bruks til generering av selftester.
         */
        public static Ping avskrudd(PingMetadata metadata) {
            return new Ping(metadata).setErAvskrudd(true);
        }

        /**
         * @param metadata Metadata om den pingbare-ressursen. Inneholder endepunkt, beskrivelse og om det er
         *                 en kritisk avhengighet eller ikke.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(PingMetadata metadata, Throwable feil) {
            return new Ping(metadata)
                    .setFeil(feil);
        }

        /**
         * @param metadata Metadata om den pingbare-ressursen. Inneholder endepunkt, beskrivelse og om det er
         *                 en kritisk avhengighet eller ikke.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(PingMetadata metadata, String feilmelding) {
            return new Ping(metadata)
                    .setFeilmelding(feilmelding);
        }

        /**
         * @param metadata Metadata om den pingbare-ressursen. Inneholder endepunkt, beskrivelse og om det er
         *                 en kritisk avhengighet eller ikke.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(PingMetadata metadata, String feilmelding, Throwable feil) {
            return new Ping(metadata)
                    .setFeilmelding(feilmelding)
                    .setFeil(feil);
        }

        /**
         * Metadata om en pingable.
         */
        public static class PingMetadata {
            String id;
            String endepunkt;
            String beskrivelse;
            boolean kritisk;

            /**
             * Metadata om en pingable.
             * @param id Unik id for pingable
             * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
             *                  database connection-string e.l.
             * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
             *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
             * @param kritisk Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
             *                fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
             *                kun returnere warning i stede for error.
             */
            public PingMetadata(String id, String endepunkt, String beskrivelse, boolean kritisk) {
                this.id = id;
                this.endepunkt = endepunkt;
                this.beskrivelse = beskrivelse;
                this.kritisk = kritisk;
            }

            @Deprecated
            public PingMetadata(String endepunkt, String beskrivelse, boolean kritisk) {
                this(UUID.randomUUID().toString(),endepunkt,beskrivelse,kritisk);
            }

            public String getId() {
                return id;
            }

            public String getEndepunkt() {
                return endepunkt;
            }

            public String getBeskrivelse() {
                return beskrivelse;
            }

            public boolean isKritisk() {
                return kritisk;
            }
        }
    }
}
