package no.nav.sbl.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public abstract class WhereClause {
    public static WhereClause equals(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.EQUALS, field, value);
    }
    public static WhereClause gt(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.GT, field, value);
    }
    public static WhereClause gteq(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.GTEQ, field, value);
    }
    public static WhereClause lt(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.LT, field, value);
    }
    public static WhereClause lteq(String field, Object value) {
        return new ComparativeWhereClause(WhereOperator.LTEQ, field, value);
    }
    public static WhereClause in(String field, Collection<?> objects) {
        return WhereIn.of(field, objects);
    }

    public static WhereClause isNotNull(String field) {
        return WhereIsNotNull.of(field);
    }

    public static WhereClause isNull(String field) {
        return WhereIsNull.of(field);
    }

    public WhereClause and(WhereClause other) {
        return new LogicalWhereClause(WhereOperator.AND, this, other);
    }

    public WhereClause andIf(WhereClause other, boolean add) {
        return add ? and(other) : this;
    }

    public WhereClause or(WhereClause other) {
        return new LogicalWhereClause(WhereOperator.OR, this, other);
    }

    public abstract int applyTo(PreparedStatement ps, int index) throws SQLException;

    public abstract String toSql();

    public abstract boolean appliesTo(String key);

    public abstract List<String> getFields();
}
