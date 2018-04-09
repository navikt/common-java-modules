package no.nav.sbl.sql.where;


import java.util.List;

import static java.util.Collections.singletonList;

public class WhereIsNull extends WhereClause {
    private String field;


    WhereIsNull(String field) {
        this.field = field;
    }

    static WhereIsNull of(String field) {
        return new WhereIsNull(field);
    }

    @Override
    public Object[] getArgs() {
        return new Object[]{};
    }

    @Override
    public String toSql() {
        return String.format("%s is null", field);
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
