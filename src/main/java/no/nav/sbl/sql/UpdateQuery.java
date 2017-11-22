package no.nav.sbl.sql;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class UpdateQuery {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Object> setParams;
    private String whereParam;
    private Object whereValue;

    public UpdateQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }

    public UpdateQuery set(String param, Object value) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(String.format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, value);
        return this;
    }

    public UpdateQuery whereEquals(String whereParam, Object whereValue) {
        this.whereParam = whereParam;
        this.whereValue = whereValue;
        return this;
    }

    public Integer execute() {
        assert tableName != null;
        assert !setParams.isEmpty();

        StringBuilder sqlBuilder = new StringBuilder()
                .append("update ").append(tableName)
                .append(createSetStatement());

        if (this.whereParam != null) {
            sqlBuilder.append(" where ").append(whereParam).append(" = ?");
        }

        String sql = sqlBuilder.toString();

        return timedPreparedStatement(sql,()-> db.update(sql, createSqlArgumentArray()));
    }

    private Object[] createSqlArgumentArray() {
        int argumentsSize = whereValue != null ? setParams.size() + 1 : setParams.size();
        Object[] arguments = new Object[argumentsSize];

        int index = 0;
        for (Object value : setParams.values()) {
            arguments[index] = value;
            index = index + 1;
        }
        if (whereValue != null) {
            arguments[index] = whereValue;
        }

        return arguments;
    }

    private String createSetStatement() {
        return " set " + setParams.entrySet().stream()
                .map(Map.Entry::getKey)
                .map(SqlUtils.append(" = ?"))
                .collect(joining(", "));
    }

}
