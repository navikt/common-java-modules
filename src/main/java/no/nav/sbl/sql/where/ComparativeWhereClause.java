package no.nav.sbl.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static java.util.Collections.singletonList;

public class ComparativeWhereClause extends WhereClause {
    private final WhereOperator operator;
    private final String field;
    private final Object value;

    public ComparativeWhereClause(WhereOperator operator, String field, Object value) {
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        ps.setObject(index, this.value);
        return index + 1;
    }

    @Override
    public String toSql() {
        return String.format("%s %s ?", this.field, this.operator.sql);
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
