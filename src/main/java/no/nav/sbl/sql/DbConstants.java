package no.nav.sbl.sql;

public enum DbConstants {
    CURRENT_TIMESTAMP("CURRENT_TIMESTAMP");

    public final String sql;

    DbConstants(String sql) {
        this.sql = sql;
    }
}
