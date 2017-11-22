package no.nav.sbl.sql.where;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LogicalWhereClause extends WhereClause {
    private final WhereOperator operator;
    private final WhereClause wc1;
    private final WhereClause wc2;

    public LogicalWhereClause(WhereOperator operator, WhereClause wc1, WhereClause wc2) {
        this.operator = operator;
        this.wc1 = wc1;
        this.wc2 = wc2;

    }

    @Override
    public int applyTo(PreparedStatement ps, int index) throws SQLException {
        int i = this.wc1.applyTo(ps, index);
        return this.wc2.applyTo(ps, i);
    }

    @Override
    public String toSql() {
        return String.format("(%s) %s (%s)", this.wc1.toSql(), this.operator.sql, this.wc2.toSql());
    }

    @Override
    public boolean appliesTo(String key) {
        return wc1.appliesTo(key) || wc2.appliesTo(key);
    }
}
