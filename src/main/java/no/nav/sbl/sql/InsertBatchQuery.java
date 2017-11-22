package no.nav.sbl.sql;

import io.vavr.Tuple;
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

import static java.util.Collections.nCopies;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class InsertBatchQuery<T> {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Tuple2<Class, Function<T, Object>>> values;

    public InsertBatchQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.values = new LinkedHashMap<>();
    }

    public InsertBatchQuery<T> add(String param, Function<T, Object> paramValue, Class type) {
        if (this.values.containsKey(param)) {
            throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
        }
        this.values.put(param, Tuple.of(type, paramValue));
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
                for (Tuple2<Class, Function<T, Object>> param : values.values()) {
                    setParam(ps, j++, param._1(), param._2.apply(t));
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
        String valueParms = StringUtils.join(nCopies(values.size(), "?"), ",");
        return String.format("insert into %s (%s) values (%s)", tableName, columns, valueParms);
    }
}
