package no.nav.sbl.sql.where;


import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WhereIsNotNull extends WhereClause{
    private String field;


    public WhereIsNotNull(String field) {
        this.field = field;
    }

    public static WhereIsNotNull of(String field) {
        return new WhereIsNotNull(field);
    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        return index;
    }

    @Override
    public String toSql() {
        return String.format("%s is not null", field);
    }

    @Override
    public boolean appliesTo(String key) {
        return key.equals(field);
    }
}
