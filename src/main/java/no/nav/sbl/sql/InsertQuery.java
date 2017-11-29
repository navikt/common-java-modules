package no.nav.sbl.sql;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class InsertQuery {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Value> insertParams;

    public InsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.insertParams = new LinkedHashMap<>();
    }

    public InsertQuery value(String columnName, Object value) {
        this.insertParams.put(columnName, Value.of(value));
        return this;
    }

    public InsertQuery value(String columnName, DbConstants value) {
        this.insertParams.put(columnName, Value.of(value));
        return this;
    }

    public int execute() {
        String sql = createSqlStatement();
        Object[] args = insertParams.values()
                .stream()
                .filter(Value::hasPlaceholder)
                .map(Value::getSql)
                .collect(Collectors.toList())
                .toArray();

        return timedPreparedStatement(sql, () -> db.update(sql, args));
    }

    private String createSqlStatement() {
        String columns = StringUtils.join(insertParams.keySet(), ",");
        String values = insertParams
                .values()
                .stream()
                .map(Value::getValuePlaceholder)
                .collect(joining(","));


        return String.format("insert into %s (%s) values (%s)", tableName, columns, values);
    }
}
