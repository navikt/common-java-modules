package no.nav.sbl.jdbc;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TransactorTest {

    private JdbcTemplate jdbcTemplate = TestUtils.jdbcTemplate();
    private Database database = new Database(jdbcTemplate);
    private Transactor transactor = new Transactor(new DataSourceTransactionManager(jdbcTemplate.getDataSource()));

    @Test
    public void inTransaction__utfører_callback_transaksjonelt() {
        jdbcTemplate.execute("CREATE TABLE TEST( ID NUMBER(19) NOT NULL PRIMARY KEY )");

        transactor.inTransaction(() -> {
            database.update("INSERT INTO TEST (ID) VALUES (1)");
            database.update("INSERT INTO TEST (ID) VALUES (2)");
        });

        assertThatThrownBy(()->{

            transactor.inTransaction(() -> {
                database.update("INSERT INTO TEST (ID) VALUES (3)");
                database.update("INSERT INTO TEST (ID) VALUES (4)");

                database.update("INSERT INTO TEST (ID) VALUES (1)");
            });

        }).hasCauseInstanceOf(SQLException.class);

        assertThatThrownBy(()->{

            transactor.inTransaction(() -> {
                database.update("INSERT INTO TEST (ID) VALUES (5)");
                database.update("INSERT INTO TEST (ID) VALUES (6)");

                throw new IOException("denne er checked!");
            });

        }).hasCauseInstanceOf(IOException.class);

        List<Integer> integers = database.query("SELECT ID FROM TEST", (rs) -> rs.getInt("ID"));
        assertThat(integers).containsExactly(1, 2);
    }


}