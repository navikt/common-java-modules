package no.nav.sbl.dialogarena.types;

import java.io.Serializable;

/**
 * Implementeres av komponenter skal overvåkes av selftest
 */
public interface Pingable {

    /**
     * Denne metoden må implementeres, og brukes til å sjekke et om en avhengighet er oppe. Det er viktig
     * at man fanger opp eventuelle exceptions i koden, da uhåndterte exceptions vil føre til at selftest-siden
     * returnerer status 500.
     * @return En vellykket eller feilet ping-respons.
     */
    Ping ping();

    final class Ping implements Serializable {

        private String endepunkt;
        private String beskrivelse;
        private String feilmelding;
        private Throwable feil;
        private boolean kritisk;
        private long responstid = -1;

        public String getEndepunkt() {
            return endepunkt;
        }

        public String getBeskrivelse() {
            return beskrivelse;
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

        public String getAarsak() {
            if (this.getFeilmelding() != null) {
                return this.getFeilmelding();
            } else if (this.getFeil() != null) {
                return this.getFeil().getMessage();
            }
            return "";
        }

        public Ping setResponstid(Long responstid) {
            this.responstid = responstid;
            return this;
        }

        public boolean erKritisk() {
            return this.kritisk;
        }

        Ping setResponstid(long responstid) {
            this.responstid = responstid;
            return this;
        }

        Ping setEndepunkt(String endepunkt) {
            this.endepunkt = endepunkt;
            return this;
        }

        Ping setBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        Ping setErKritisk(boolean kritisk) {
            this.kritisk = kritisk;
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

        private Ping(String endepunkt, String beskrivelse, boolean erKritisk) {
            this.setEndepunkt(endepunkt)
                    .setErKritisk(erKritisk)
                    .setBeskrivelse(beskrivelse);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param erKritisk Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                  fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                  kun returnere warning i stede for error.
         * @return Et vellykket pingresultat som kan bruks til generering av selftester.
         */
        public static Ping lyktes(String endepunkt, String beskrivelse, boolean erKritisk) {
            return new Ping(endepunkt, beskrivelse, erKritisk);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param erKritisk Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                  fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                  kun returnere warning i stede for error.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean erKritisk, Throwable feil) {
            return new Ping(endepunkt, beskrivelse, erKritisk)
                    .setFeil(feil);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param erKritisk Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                  fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                  kun returnere warning i stede for error.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean erKritisk, String feilmelding) {
            return new Ping(endepunkt, beskrivelse, erKritisk)
                    .setFeilmelding(feilmelding);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param erKritisk Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                  fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                  kun returnere warning i stede for error.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean erKritisk, String feilmelding, Throwable feil) {
            return new Ping(endepunkt, beskrivelse, erKritisk)
                    .setFeilmelding(feilmelding)
                    .setFeil(feil);
        }
    }
}
