package no.nav.sbl.sql;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.nCopies;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

public class InsertQuery {
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Object> insertParams;

    public InsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.insertParams = new LinkedHashMap<>();
    }

    public InsertQuery value(String columnName, Object value) {
        this.insertParams.put(columnName, value);
        return this;
    }

    public int execute() {
        String sql = createSqlStatement();
        return timedPreparedStatement(sql,() -> db.update(sql, insertParams.values().toArray()));
    }

    private String createSqlStatement() {
        String columns = StringUtils.join(insertParams.keySet(), ",");
        String values = StringUtils.join(nCopies(insertParams.size(), "?"), ",");
        return String.format("insert into %s (%s) values (%s)", tableName, columns, values);
    }
}
