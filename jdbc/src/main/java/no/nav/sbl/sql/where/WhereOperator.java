package no.nav.sbl.sql.where;

public enum WhereOperator {
    EQUALS("="),
    AND("AND"),
    OR("OR"),
    IN("IN"),
    GT(">"),
    GTEQ(">="),
    LT("<"),
    LTEQ("<=");

    public final String sql;

    WhereOperator(String sql) {
        this.sql = sql;
    }
}
