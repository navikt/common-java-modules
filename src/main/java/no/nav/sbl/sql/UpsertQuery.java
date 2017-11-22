package no.nav.sbl.sql;

import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

@Slf4j
public class UpsertQuery {
    private final String upsertTemplate = "MERGE INTO %s USING dual ON (%s) WHEN MATCHED THEN %s WHEN NOT MATCHED THEN %s";
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Object> setParams;
    private WhereClause where;

    public UpsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }
    public UpsertQuery set(String param, Object value) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, value);
        return this;
    }

    public UpsertQuery where(WhereClause where) {
        this.where = where;
        return this;
    }

    public Boolean execute() {
        if (this.tableName == null || this.where == null || this.setParams.isEmpty()) {
            throw new SqlUtilsException(
                    "I need more data to create a sql-statement. " +
                            "Did you remember to specify table name, " +
                            "what columns to set and a where clause?"
            );
        }

        String upsertStatement = createUpsertStatement();
        return
                db.execute(upsertStatement, (PreparedStatementCallback<Boolean>) ps -> {
                    int index = 1;

                    index = this.where.applyTo(ps, index);

                    // For updatequery
                    for (Map.Entry<String, Object> entry : this.setParams.entrySet()) {
                        if (!this.where.appliesTo(entry.getKey())) {
                            ps.setObject(index++, entry.getValue());
                        }
                    }

                    // For insertquery
                    for (Map.Entry<String, Object> entry : this.setParams.entrySet()) {
                        ps.setObject(index++, entry.getValue());
                    }

                    log.debug(String.format("[UpsertQuery] Sql: %s \n [UpsertQuery] Params: %s", upsertStatement, this.setParams));
                    Boolean result;
                    try {
                        result = timedPreparedStatement(upsertStatement, ps::execute);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                    return result;
                });
    }

    private String createUpsertStatement() {
        return format(
                upsertTemplate,
                this.tableName,
                this.where.toSql(),
                this.createUpdateStatement(),
                this.createInsertStatement()
        );
    }

    private String createUpdateStatement() {
        return format("UPDATE SET %s", this.createSetStatement());
    }

    private String createSetStatement() {
        return setParams
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .filter((key) -> !this.where.appliesTo(key))
                .map(SqlUtils.append(" = ?"))
                .collect(joining(", "));
    }

    private String createInsertStatement() {
        return format("INSERT (%s) VALUES (%s)", this.createInsertFields(), this.createInsertValues());
    }

    private String createInsertFields() {
        return setParams
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(joining(", "));
    }

    private String createInsertValues() {
        return setParams
                .entrySet().stream()
                .map((entry) -> "?")
                .collect(joining(", "));
    }

    @Override
    public String toString() {
        return createUpsertStatement();
    }
}
