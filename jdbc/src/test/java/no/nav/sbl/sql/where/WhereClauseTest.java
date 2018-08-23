package no.nav.sbl.sql.where;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class WhereClauseTest {
    @Test
    public void appliesToTest() {
        assertThat(WhereClause.equals("felt1", 0).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt2", 0).appliesTo("felt1")).isFalse();

        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt2")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).and(WhereClause.equals("felt2", 0)).appliesTo("felt3")).isFalse();

        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt1")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt2")).isTrue();
        assertThat(WhereClause.equals("felt1", 0).or(WhereClause.equals("felt2", 0)).appliesTo("felt3")).isFalse();
    }

    @Test
    public void whereOr() {
        WhereClause whereClause1 = WhereClause.equals("felt1","verdi1");
        WhereClause whereClause2 = WhereClause.equals("felt2","verdi2");
        WhereClause whereClause3 = WhereClause.equals("felt3","verdi3");
        assertThat(whereClause1.and(whereClause2.or(whereClause3)).toSql()).isEqualTo("(felt1 = ?) AND ((felt2 = ?) OR (felt3 = ?))");
    }
}
