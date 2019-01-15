package no.nav.common.auth.openam.sbs;

public enum SecurityLevel {
    Level1(1),
    Level2(2),
    Level3(3),
    Level4(4),
    Ukjent(-1);

    private int securityLevel;

    SecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }
}
