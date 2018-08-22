package no.nav.sbl.sql;

import no.nav.sbl.sql.mapping.QueryMapping;
import no.nav.sbl.sql.mapping.SqlRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.function.Function;

public class SqlUtils {
    static Function<String, String> append(final String suffix) {
        return (String value) -> value + suffix;
    }

    public static UpdateQuery update(JdbcTemplate db, String tableName) {
        return new UpdateQuery(db, tableName);
    }

    public static <S> UpdateBatchQuery<S> updateBatch(JdbcTemplate db, String tableName, Class<S> cls) {
        return new UpdateBatchQuery<>(db, tableName);
    }

    public static InsertQuery insert(JdbcTemplate db, String tableName) {
        return new InsertQuery(db, tableName);
    }

    public static UpsertQuery upsert(JdbcTemplate db, String tableName) {
        return new UpsertQuery(db, tableName);
    }

    public static <T> SelectQuery<T> select(JdbcTemplate db, String tableName, SQLFunction<ResultSet, T> mapper) {
        return new SelectQuery<>(db, tableName, mapper);
    }

    public static <T extends SqlRecord> SelectQuery<T> select(JdbcTemplate db, String tableName, Class<T> recordClass) {
        QueryMapping<T> querymapping = QueryMapping.of(recordClass);

        SelectQuery<T> selectQuery = new SelectQuery<>(db, tableName, querymapping::createMapper);
        querymapping.applyColumn(selectQuery);

        return selectQuery;
    }

    public static SelectQuery<Long> nextFromSeq(JdbcTemplate db, String sekvens) {
        return select(db, "dual", resultSet -> resultSet.getLong(1))
                .column(String.format("%s.NEXTVAL", sekvens));
    }

    public static DeleteQuery delete(JdbcTemplate db, String tableName) {
        return new DeleteQuery(db, tableName);
    }

}
