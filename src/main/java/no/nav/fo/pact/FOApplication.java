package no.nav.fo.pact;

public enum FOApplication {
    VEILARBPERSON("veilarbperson"),
    VEILARBPERSONFS("veilarbpersonfs");


    private String foName;

    FOApplication(String foName) {
        this.foName = foName;
    }

    public String getFoName() {
        return foName;
    }

    @Override
    public String toString() {
        return getFoName();
    }
}
