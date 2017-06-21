package no.nav.apiapp.selftest;

public class HelsesjekkMetadata {
    private String endepunkt;
    private String beskrivelse;
    private boolean kritisk;

    public HelsesjekkMetadata(String endepunkt, String beskrivelse, boolean erKritisk) {
        this.endepunkt = endepunkt;
        this.beskrivelse = beskrivelse;
        this.kritisk = erKritisk;
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
