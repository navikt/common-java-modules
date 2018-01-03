package no.nav.sbl.sql;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import lombok.SneakyThrows;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static no.nav.sbl.sql.Utils.timedPreparedStatement;


public class SelectQuery<T> {
    private DataSource ds;
    private String tableName;
    private List<String> columnNames;
    private Function<ResultSet, T> mapper;
    private WhereClause where;
    private OrderClause order;
    private Integer offset;
    private Integer rowCount;
    private Tuple3<String, String, String> leftJoinOn;

    SelectQuery(DataSource ds, String tableName, Function<ResultSet, T> mapper) {
        this.ds = ds;
        this.tableName = tableName;
        this.columnNames = new ArrayList<>();
        this.mapper = mapper;
    }

    public SelectQuery<T> leftJoinOn(String joinTableName, String leftOn, String rightOn) {
        leftJoinOn = Tuple.of(joinTableName, leftOn, rightOn);
        return this;
    }

    public SelectQuery<T> column(String columnName) {
        this.columnNames.add(columnName);
        return this;
    }

    public SelectQuery<T> where(WhereClause where) {
        this.where = where;
        return this;
    }

    public SelectQuery<T> orderBy(OrderClause order) {
        this.order = order;
        return this;
    }

    public SelectQuery<T> limit(int offset, int rowCount) {
        this.offset = offset;
        this.rowCount = rowCount;
        return this;
    }

    public SelectQuery<T> limit(int rowCount) {
        return limit(0, rowCount);
    }

    @SneakyThrows
    public T execute() {
        validate();
        String sql = createSelectStatement();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (where != null) {
                where.applyTo(ps, 1);
            }
            ResultSet resultSet = timedPreparedStatement(sql, ps::executeQuery);
            if(!resultSet.next()) {
                return null;
            }
            return mapper.apply(resultSet);
        }
    }

    @SneakyThrows
    public List<T> executeToList() {
        validate();
        String sql = createSelectStatement();
        List<T> data = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (where != null) {
                where.applyTo(ps, 1);
            }
            ResultSet resultSet = timedPreparedStatement(sql, ps::executeQuery);

            while(resultSet.next()) {
                data.add(mapper.apply(resultSet));
            }
            return data;
        }
    }

    private void validate() {
        if (tableName == null || columnNames.isEmpty()) {
            throw new SqlUtilsException(
                    "I need more data to create a sql-statement. " +
                    "Did you remember to specify table and columns?"
            );
        }

        if (mapper == null) {
            throw new SqlUtilsException("I need a mapper function in order to return the right data type.");
        }
    }

    private String createSelectStatement() {
        StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT ");

        columnNames.stream()
                .flatMap(x -> Stream.of(", ", x))
                .skip(1)
                .forEach(sqlBuilder::append);

        sqlBuilder
                .append(" ")
                .append("FROM ")
                .append(tableName);

        if(Objects.nonNull(leftJoinOn)) {
            sqlBuilder.append(String.format(" LEFT JOIN %s ON %s.%s = %s.%s",
                    leftJoinOn._1,
                    tableName,
                    leftJoinOn._2,
                    leftJoinOn._1,
                    leftJoinOn._3) );
        }

        if (this.where != null) {
            sqlBuilder
                    .append(" WHERE ");

            sqlBuilder
                    .append(this.where.toSql());
        }

        if (this.order != null) {
            sqlBuilder.append(this.order.toSql());
        }

        if (this.offset != null) {
            sqlBuilder.append(String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, rowCount));
        }

        return sqlBuilder.toString();
    }

    @Override
    public String toString() {
        return createSelectStatement();
    }

}
