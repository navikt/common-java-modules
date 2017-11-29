package no.nav.sbl.sql;

import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class InsertBatchQuery<T> {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Value> values;

    public InsertBatchQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.values = new LinkedHashMap<>();
    }

    public InsertBatchQuery<T> add(String param, Function<T, Object> paramValue, Class type) {
        return this.add(param, new Value.FunctionValue(type, paramValue));
    }

    public InsertBatchQuery<T> add(String param, DbConstants value) {
        return this.add(param, Value.of(value));
    }

    public InsertBatchQuery<T> add(String param, Value value) {
        if (this.values.containsKey(param)) {
            throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
        }
        this.values.put(param, value);
        return this;
    }

    public int[] execute(List<T> data) {
        if (data.isEmpty()) {
            return null;
        }
        String sql = createSqlStatement();
        return timedPreparedStatement(sql, () -> db.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                T t = data.get(i);

                int j = 1;
                for (Value param : values.values()) {
                    if (param instanceof Value.FunctionValue) {
                        Value.FunctionValue<T> functionValue = (Value.FunctionValue) param;
                        Tuple2<Class, Function<T, Object>> config = functionValue.getSql();

                        setParam(ps, j++, config._1(), config._2.apply(t));

                    }
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
        } else if (Boolean.class == type) {
            ps.setBoolean(i, (Boolean) value);
        } else if (Integer.class == type) {
            ps.setInt(i, (Integer) value);
        }
    }

    private String createSqlStatement() {
        String columns = StringUtils.join(values.keySet(), ",");
        String valueParams = values
                .values()
                .stream()
                .map(Value::getValuePlaceholder)
                .collect(joining(","));
        return String.format("insert into %s (%s) values (%s)", tableName, columns, valueParams);
    }
}
