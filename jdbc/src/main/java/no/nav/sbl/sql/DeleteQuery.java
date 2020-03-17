package no.nav.sbl.sql;

import lombok.SneakyThrows;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

public class DeleteQuery {
    private final JdbcTemplate db;
    private final String tableName;
    private WhereClause where;

    DeleteQuery(JdbcTemplate db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    public DeleteQuery where(WhereClause where) {
        this.where = where;
        return this;
    }

    @SneakyThrows
    public int execute() {
        if (tableName == null || this.where == null) {
            throw new SqlUtilsException(
                    "I need more data to create a sql-statement. " +
                            "Did you remember to specify table and a where clause?"
            );
        }

        String sql = createDeleteStatement();

        return db.update(sql, this.where.getArgs());
    }

    private String createDeleteStatement() {
        return String.format(
                "DELETE FROM %s WHERE %s",
                tableName,
                this.where.toSql()
        );
    }

    @Override
    public String toString() {
        return createDeleteStatement();
    }
}
