package no.nav.sbl.sql.where;



import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class WhereInTest {

    @Test
    public void whereInSql() {
        WhereIn whereIn = new WhereIn("FIELD", asList("value1", "value2"));
        assertThat(whereIn.toSql()).isEqualTo("FIELD IN (?,?)");
    }



}
