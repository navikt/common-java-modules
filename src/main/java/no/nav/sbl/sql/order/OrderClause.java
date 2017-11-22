package no.nav.sbl.sql.order;

public class OrderClause {
    public static OrderClause asc(String field) {
        return new OrderClause(OrderOperator.ASC, field);
    }

    public static OrderClause desc(String field) {
        return new OrderClause(OrderOperator.DESC, field);
    }

    private final OrderOperator operator;
    private final String field;

    private OrderClause(OrderOperator operator, String field) {
        this.operator = operator;
        this.field = field;
    }

    public String toSql() {
        return String.format(" ORDER BY %s %s", this.field, this.operator.sql);
    }
}
