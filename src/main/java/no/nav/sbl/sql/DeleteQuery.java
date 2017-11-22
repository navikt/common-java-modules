package no.nav.sbl.sql;

import lombok.SneakyThrows;
import no.nav.sbl.sql.where.WhereClause;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static no.nav.sbl.sql.Utils.timedPreparedStatement;


public class DeleteQuery {
    private final DataSource ds;
    private final String tableName;
    private WhereClause where;

    DeleteQuery(DataSource ds, String tableName) {
        this.ds = ds;
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

        int result;
        String sql = createDeleteStatement();
        try (Connection conn = ds.getConnection()) {

            PreparedStatement ps = timedPreparedStatement(sql,() ->conn.prepareStatement(sql));
            where.applyTo(ps, 1);

            result = ps.executeUpdate();

        } catch (SQLException e) {
            throw new SqlUtilsException(e);
        }
        return result;
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
