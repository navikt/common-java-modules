package no.nav.sbl.sql.where;


import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WhereIsNull extends WhereClause{
    private String field;


    public WhereIsNull(String field) {
        this.field = field;
    }

    public static WhereIsNull of(String field) {
        return new WhereIsNull(field);
    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        return index;
    }

    @Override
    public String toSql() {
        return String.format("%s is null", field);
    }

    @Override
    public boolean appliesTo(String key) {
        return key.equals(field);
    }
}
