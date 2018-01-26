package no.nav.sbl.sql.where;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static java.util.Collections.singletonList;

public class WhereIsNotNull extends WhereClause{
    private String field;


    WhereIsNotNull(String field) {
        this.field = field;
    }

    static WhereIsNotNull of(String field) {
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

    @Override
    public List<String> getFields() {
        return singletonList(field);
    }
}
