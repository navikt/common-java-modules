package no.nav.sbl.sql;

import io.vavr.Tuple2;
import no.nav.sbl.sql.value.FunctionValue;
import no.nav.sbl.sql.value.Value;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class UpdateBatchQuery<T> {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Value> setParams;
    private Function<T, WhereClause> whereClause;

    public UpdateBatchQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }

    public UpdateBatchQuery<T> add(String param, Function<T, Object> paramValue, Class type) {
        return this.add(param, new FunctionValue<>(type, paramValue));
    }

    public UpdateBatchQuery<T> add(String param, DbConstants value) {
        return this.add(param, Value.of(value));
    }

    public UpdateBatchQuery<T> add(String param, Value value) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, value);
        return this;
    }

    public UpdateBatchQuery<T> addWhereClause(Function<T, WhereClause> whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    public int[] execute(List<T> data) {
        if (data.isEmpty()) {
            return null;
        }
        String sql = createSql(data.get(0));
        return timedPreparedStatement(sql, () -> db.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                T t = data.get(i);

                int j = 1;
                for (Value param : setParams.values()) {
                    if (param instanceof FunctionValue) {
                        FunctionValue<T> functionValue = (FunctionValue) param;
                        Tuple2<Class, Function<T, Object>> config = functionValue.getSql();

                        setParam(ps, j++, config._1(), config._2.apply(t));
                    }
                }
                if (Objects.nonNull(whereClause)) {
                    whereClause.apply(t).applyTo(ps, j);
                }
            }

            @Override
            public int getBatchSize() {
                return data.size();
            }
        }));
    }

    static void setParam(PreparedStatement ps, int i, Class type, Object value) throws SQLException {
        if (String.class == type) {
            ps.setString(i, (String) value);
        } else if (Timestamp.class == type) {
            ps.setTimestamp(i, (Timestamp) value);
        } else if (Integer.class == type) {
            if (value != null) {
                ps.setInt(i, (Integer) value);
            } else {
                ps.setInt(i, -1);
            }
        } else if (Boolean.class == type) {
            ps.setBoolean(i, (Boolean) value);
        }
    }

    String createSql(T t) {
        StringBuilder sqlBuilder = new StringBuilder()
                .append("update ").append(tableName)
                .append(createSetStatement());

        if (Objects.nonNull(whereClause)) {
            sqlBuilder.append(" where ").append(whereClause.apply(t).toSql());
        }

        return sqlBuilder.toString();
    }

    private String createSetStatement() {
        return " set " + setParams.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue().getValuePlaceholder())
                .collect(joining(", "));
    }
}
