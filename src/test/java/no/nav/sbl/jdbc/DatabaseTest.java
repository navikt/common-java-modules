package no.nav.sbl.jdbc;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DatabaseTest {

    private Database database = new Database(TestUtils.jdbcTemplate());

    @Test
    public void update_og_query() {
        database.update("CREATE TABLE test ( id number(19), tekst varchar(255)  )");
        database.update("INSERT INTO test ( id, tekst ) VALUES ( ?, ? )", 1, "en test");
        database.update("INSERT INTO test ( id, tekst ) VALUES ( ?, ? )", 2, "enda en test");

        String tekst = database.queryForObject("SELECT tekst FROM test WHERE id = ? ", resultSet -> resultSet.getString("tekst"), 1);
        assertThat(tekst).isEqualTo("en test");
        List<String> tekster = database.query("SELECT tekst FROM test", resultSet -> resultSet.getString(1));
        assertThat(tekster).isEqualTo(Arrays.asList(
                "en test",
                "enda en test"
        ));
    }

    @Test
    public void nesteFraSekvens() {
        database.update("CREATE SEQUENCE MIN_SEKVENS");
        assertThat(database.nesteFraSekvens("MIN_SEKVENS") + 1).isEqualTo(database.nesteFraSekvens("MIN_SEKVENS"));
    }

}