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
        private boolean kritisk = true;
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

        private Ping(String endepunkt, String beskrivelse) {
            this.setEndepunkt(endepunkt)
                    .setBeskrivelse(beskrivelse);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param responstid Sjekkens responstid.
         * @return Et vellykket pingresultat som kan bruks til generering av selftester.
         */
        public static Ping lyktes(String endepunkt, String beskrivelse, long responstid) {
            return new Ping(endepunkt, beskrivelse)
                    .setResponstid(responstid);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param kritiskFeil Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                    fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                    kun returnere warning i stede for error.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean kritiskFeil, Throwable feil) {
            return new Ping(endepunkt, beskrivelse)
                    .setErKritisk(kritiskFeil)
                    .setFeil(feil);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param kritiskFeil Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                    fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                    kun returnere warning i stede for error.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean kritiskFeil, String feilmelding) {
            return new Ping(endepunkt, beskrivelse)
                    .setFeilmelding(feilmelding)
                    .setErKritisk(kritiskFeil);
        }

        /**
         * @param endepunkt Presis beskrivelse av endepunktet som kalles. Eksempelvis full URI ved SOAP/REST-kall,
         *                  database connection-string e.l.
         * @param beskrivelse En kort beskrivelse av hva selftesten gjør. Denne beskrivelsen bør være god nok til at
         *                    den kan forstås av folk uten detaljkunnskaper om applikasjonen.
         * @param kritiskFeil Hvor vidt dette er en kritisk feil eller ikke. En ukritisk feil betyr at applikasjonen
         *                    fint kan kjøre selv om tjenesten er nede. Har en selftest ingen kritiske feil vil den
         *                    kun returnere warning i stede for error.
         * @param feilmelding En beskrivende feilmelding av hva som er galt.
         * @param feil Exceptionen som trigget feilen. I selftestene blir stacktracen vist om denne er lagt ved.
         * @return Et feilet pingresultat som kan bruks til generering av selftester.
         */
        public static Ping feilet(String endepunkt, String beskrivelse, boolean kritiskFeil, String feilmelding, Throwable feil) {
            return new Ping(endepunkt, beskrivelse)
                    .setFeilmelding(feilmelding)
                    .setErKritisk(kritiskFeil)
                    .setFeil(feil);
        }
    }
}
