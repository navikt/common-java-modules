package no.nav.sbl.sql;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.sbl.jdbc.TestUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class UpsertQueryTest {
    public final static String TESTTABLE1 = "TESTTABLE1";
    public final static String ID = "ID";
    public final static String NAVN = "NAVN";
    public final static String DEAD = "DEAD";
    public final static String UPDATED = "UPDATED";
    public final static String CREATED = "CREATED";

    private DataSource ds;
    private JdbcTemplate db;

    @Before
    public void setup() {
        db = TestUtils.jdbcTemplate();
        ds = db.getDataSource();
        db.update("CREATE TABLE TESTTABLE1 (\n" +
                "  ID VARCHAR(255) NOT NULL,\n" +
                "  NAVN VARCHAR(255) NOT NULL,\n" +
                "  DEAD VARCHAR(20),\n" +
                "  UPDATED TIMESTAMP,\n" +
                "  CREATED TIMESTAMP,\n" +
                "  PRIMARY KEY(ID)\n" +
                ")");
    }

    @Test
    public void upsertQuery() {
        String oppdatertNavn1 = "oppdatert navn1";
        String oppdatertNavn2 = "oppdatert navn2";

        // Goes to insert
        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, oppdatertNavn1)
                .set(ID, "007")
                .where(WhereClause.equals(ID, "007"))
                .execute();

        Testobject retrieved1 = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute();
        assertThat(retrieved1.getNavn()).isEqualTo(oppdatertNavn1);

        // Goes to update
        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, oppdatertNavn2)
                .set(ID, "007")
                .where(WhereClause.equals(ID, "007"))
                .execute();

        Testobject retrieved2 = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute();
        assertThat(retrieved2.getNavn()).isEqualTo(oppdatertNavn2);
    }

    @Test
    public void upsertMatchTest() {
        String id = "001";
        String navn = "navn";
        String oppdatertnavn = "oppdatertnavn";
        Timestamp now = new Timestamp(0);
        Timestamp later = new Timestamp(12 * 3600 * 1000);

        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, navn)
                .set(DEAD, true)
                .set(CREATED, now)
                .set(UPDATED, now)
                .set(ID, id)
                .where(WhereClause.equals(ID, id))
                .execute();

        Testobject retrieved = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, id)).execute();
        assertThat(retrieved.isDead()).isTrue();
        assertThat(retrieved.getNavn()).isEqualTo(navn);
        assertThat(retrieved.getCreated()).isEqualTo(now);
        assertThat(retrieved.getUpdated()).isEqualTo(now);

        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, oppdatertnavn)
                .set(DEAD, false, UpsertQuery.ApplyTo.INSERT)
                .set(CREATED, later, UpsertQuery.ApplyTo.INSERT)
                .set(UPDATED, later, UpsertQuery.ApplyTo.UPDATE)
                .set(ID, id, UpsertQuery.ApplyTo.BOTH) // Redundant to explicitly say BOTH...
                .where(WhereClause.equals(ID, id))
                .execute();

        Testobject retrieved2 = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, id)).execute();

        assertThat(retrieved2.isDead()).isTrue();
        assertThat(retrieved2.getNavn()).isEqualTo(oppdatertnavn);
        assertThat(retrieved2.getCreated()).isEqualTo(now);
        assertThat(retrieved2.getUpdated()).isEqualTo(later);
    }

    @Test
    public void testAvMatches() {
        String id = "002";
        String navn = "testersen";
        String oppdatertnavn = "giftet testersen";
        Timestamp created = new Timestamp(0);
        Timestamp muchlaterTime = new Timestamp(120 * 3600 * 1000);
        Timestamp laterTime = new Timestamp(12 * 3600 * 1000);

        Testobject initial = new Testobject()
                .setId(id)
                .setNavn(navn)
                .setDead(false)
                .setCreated(created)
                .setUpdated(created);

        Testobject later = new Testobject()
                .setId(id)
                .setNavn(navn)
                .setDead(false)
                .setCreated(laterTime)
                .setUpdated(laterTime);

        Testobject muchlater = new Testobject()
                .setId(id)
                .setNavn(oppdatertnavn)
                .setDead(true)
                .setCreated(muchlaterTime)
                .setUpdated(muchlaterTime);

        upsert(initial);
        upsert(later);
        upsert(muchlater);

        Testobject retrieved = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, id)).execute();
        assertThat(retrieved.isDead()).isTrue();
        assertThat(retrieved.getNavn()).isEqualTo(oppdatertnavn);
        assertThat(retrieved.getCreated()).isEqualTo(created);
        assertThat(retrieved.getUpdated()).isEqualTo(muchlaterTime);
        assertThat(retrieved.getId()).isEqualTo(id);
    }

    @Test(expected = SqlUtilsException.class)
    public void failIfWhereClauseIsntSet() {
        SqlUtils.upsert(db, TESTTABLE1)
                .set("field2", "")
                .where(WhereClause.equals("field", ""))
                .execute();
    }

    @Test(expected = SqlUtilsException.class)
    public void failIfWhereClauseIsntSet2() {
        SqlUtils.upsert(db, TESTTABLE1)
                .set(ID, "")
                .set(DEAD, true)
                .where(WhereClause.equals(ID, "").and(WhereClause.equals(NAVN, "a")))
                .execute();
    }

    @Test(expected = SqlUtilsException.class)
    public void failIfWhereClauseFieldIsSetToUpdate() {
        SqlUtils.upsert(db, TESTTABLE1)
                .set("field", "", UpsertQuery.ApplyTo.UPDATE)
                .where(WhereClause.equals("field", ""))
                .execute();
    }

    @Test(expected = SqlUtilsException.class)
    public void failIfNoFieldsArePresentForUpdate() {
        SqlUtils.upsert(db, TESTTABLE1)
                .set(ID, "ID1", UpsertQuery.ApplyTo.INSERT)
                .set(NAVN, "navn", UpsertQuery.ApplyTo.BOTH)
                .where(WhereClause.equals(ID, "").and(WhereClause.equals(NAVN, "a")))
                .execute();
    }

    @Test
    public void testBasicFuksjonalitet() {
        String id = "ID1";
        String navn = "navn";
        Timestamp created = new Timestamp(0);
        Timestamp updated = new Timestamp(120);

        SqlUtils.upsert(db, TESTTABLE1)
                .set(ID, id, UpsertQuery.ApplyTo.INSERT)
                .set(NAVN, navn, UpsertQuery.ApplyTo.BOTH)
                .set(UPDATED, updated, UpsertQuery.ApplyTo.BOTH)
                .set(CREATED, created, UpsertQuery.ApplyTo.INSERT)
                .where(WhereClause.equals(ID, "").and(WhereClause.equals(NAVN, "a")))
                .execute();

        Testobject retrieved = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, id)).execute();
        assertThat(retrieved.isDead()).isFalse();
        assertThat(retrieved.getNavn()).isEqualTo(navn);
        assertThat(retrieved.getCreated()).isEqualTo(created);
        assertThat(retrieved.getUpdated()).isEqualTo(updated);
        assertThat(retrieved.getId()).isEqualTo(id);
    }

    private void upsert(Testobject testobject) {
        SqlUtils.upsert(db, TESTTABLE1)
                .set(ID, testobject.id, UpsertQuery.ApplyTo.INSERT)
                .set(NAVN, testobject.navn, UpsertQuery.ApplyTo.BOTH) // Redundant to explicitly say BOTH...
                .set(DEAD,  testobject.dead, UpsertQuery.ApplyTo.BOTH)
                .set(CREATED, testobject.created, UpsertQuery.ApplyTo.INSERT)
                .set(UPDATED, testobject.updated, UpsertQuery.ApplyTo.BOTH)
                .where(WhereClause.equals(ID, testobject.id))
                .execute();
    }

    @Data
    @Accessors(chain = true)
    public static class Testobject {
        String id;
        String navn;
        Timestamp created;
        Timestamp updated;
        boolean dead;

        public static SelectQuery<Testobject> getSelectQuery(DataSource ds, String table) {
            return SqlUtils.select(ds, table, Testobject::mapper)
                    .column(ID)
                    .column(NAVN)
                    .column(DEAD)
                    .column(CREATED)
                    .column(UPDATED);
        }

        public static Testobject mapper(ResultSet rs) throws SQLException {
            return new Testobject()
                    .setId(rs.getString(ID))
                    .setNavn(rs.getString(NAVN))
                    .setCreated(rs.getTimestamp(CREATED))
                    .setUpdated(rs.getTimestamp(UPDATED))
                    .setDead(rs.getBoolean(DEAD));
        }
    }
}
