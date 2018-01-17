package no.nav.sbl.sql;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static no.nav.sbl.sql.UpsertQuery.Match.INSERT;
import static no.nav.sbl.sql.UpsertQuery.Match.UPDATE;
import static no.nav.sbl.sql.Utils.timedPreparedStatement;

@Slf4j
public class UpsertQuery {
    private final String upsertTemplate = "MERGE INTO %s USING dual ON (%s) WHEN MATCHED THEN %s WHEN NOT MATCHED THEN %s";
    private final JdbcTemplate db;
    private final String tableName;
    private final Map<String, Tuple2<Match, Object>> setParams;
    private WhereClause where;

    public enum Match {
        UPDATE, INSERT, BOTH(UPDATE, INSERT);

        public Match[] matches;

        Match(Match... matches) {
            this.matches = matches;
        }

        public boolean appliesTo(Match update) {
            return this.equals(update) || Arrays.binarySearch(this.matches, update) >= 0;
        }
    }

    public UpsertQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
        this.setParams = new LinkedHashMap<>();
    }

    public UpsertQuery set(String param, Object value) {
        return set(param, value, Match.BOTH);
    }

    public UpsertQuery set(String param, Object value, Match match) {
        if (this.setParams.containsKey(param)) {
            throw new IllegalArgumentException(format("Param[%s] was already set.", param));
        }
        this.setParams.put(param, Tuple.of(match, value));
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
                    for (Map.Entry<String, Tuple2<Match, Object>> entry : this.setParams.entrySet()) {
                        if (!this.where.appliesTo(entry.getKey()) && skalSetParamVareMed(UPDATE).test(entry)) {
                            ps.setObject(index++, entry.getValue()._2());
                        }
                    }

                    // For insertquery
                    for (Map.Entry<String, Tuple2<Match, Object>> entry : this.setParams.entrySet()) {
                        if (skalSetParamVareMed(INSERT).test(entry)) {
                            ps.setObject(index++, entry.getValue()._2());
                        }
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
                .filter(skalSetParamVareMed(UPDATE))
                .map(Map.Entry::getKey)
                .filter((key) -> !this.where.appliesTo(key))
                .map(SqlUtils.append(" = ?"))
                .collect(joining(", "));
    }

    private Predicate<Map.Entry<String, Tuple2<Match, Object>>> skalSetParamVareMed(Match match) {
        return (Map.Entry<String, Tuple2<Match, Object>> entry) -> entry.getValue()._1().appliesTo(match);
    }

    private String createInsertStatement() {
        return format("INSERT (%s) VALUES (%s)", this.createInsertFields(), this.createInsertValues());
    }

    private String createInsertFields() {
        return setParams
                .entrySet().stream()
                .filter(skalSetParamVareMed(INSERT))
                .map(Map.Entry::getKey)
                .collect(joining(", "));
    }

    private String createInsertValues() {
        return setParams
                .entrySet().stream()
                .filter(skalSetParamVareMed(INSERT))
                .map((entry) -> "?")
                .collect(joining(", "));
    }

    @Override
    public String toString() {
        return createUpsertStatement();
    }
}
