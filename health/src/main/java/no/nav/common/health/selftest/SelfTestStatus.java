package no.nav.common.health.selftest;

public enum  SelfTestStatus {
    OK(0),
    WARNING(2),
    ERROR(1);

    public final int statusKode;

    SelfTestStatus(int statusKode) {
        this.statusKode = statusKode;
    }
}
