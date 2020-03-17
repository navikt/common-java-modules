package no.nav.sbl.sql;

import no.nav.sbl.sql.value.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class InsertQuery {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Value> insertParams;

    public InsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.insertParams = new LinkedHashMap<>();
    }

    public InsertQuery value(String columnName, Value value) {
        this.insertParams.put(columnName, value);
        return this;
    }

    public InsertQuery value(String columnName, DbConstants value) {
        return this.value(columnName, Value.of(value));
    }

    public InsertQuery value(String columnName, Object value) {
        return this.value(columnName, Value.of(value));
    }

    public int execute() {
        String sql = createSqlStatement();
        Object[] args = insertParams.values()
                .stream()
                .filter(Value::hasPlaceholder)
                .map(Value::getSql)
                .collect(Collectors.toList())
                .toArray();

        return db.update(sql, args);
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

    public String toString() {
        return createSqlStatement();
    }
}
