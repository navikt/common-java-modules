package no.nav.sbl.sql;

import no.nav.sbl.jdbc.TestUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.sbl.sql.where.WhereIn;
import no.nav.sbl.sql.where.WhereIsNotNull;
import no.nav.sbl.sql.where.WhereIsNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SqlUtilsTest {

    public final static String TESTTABLE1 = "TESTTABLE1";
    public final static String TESTTABLE2 = "TESTTABLE2";
    public final static String ID = "ID";
    public final static String NAVN = "NAVN";
    public final static String DEAD = "DEAD";
    public final static String BIRTHDAY = "BIRTHDAY";
    public final static String NUMBER_OF_PETS = "NUMBER_OF_PETS";
    public final static String ADDRESS = "ADDRESS";

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
                "  BIRTHDAY TIMESTAMP,\n" +
                "  NUMBER_OF_PETS NUMBER,\n" +
                "  PRIMARY KEY(ID)\n" +
                ")");
        db.update("CREATE TABLE TESTTABLE2 (\n" +
                "  ID VARCHAR(255) NOT NULL,\n" +
                "  ADDRESS VARCHAR(255),\n" +
                "  PRIMARY KEY(ID)\n" +
                ")"
        );
    }

    @Test
    public void insertAndSelect() {
        Testobject object = getTestobjectWithId("007");

        SqlUtils.insert(db, TESTTABLE1)
                .value(ID, object.getId())
                .value(NAVN, object.getNavn())
                .value(DEAD, object.isDead())
                .value(BIRTHDAY, object.getBirthday())
                .value(NUMBER_OF_PETS, object.getNumberOfPets())
                .execute();

        Testobject retrieved = Testobject.getSelectQuery(ds, TESTTABLE1)
                .where(WhereClause.equals(ID, object.getId()))
                .execute();

        assertThat(object).isEqualTo(retrieved);
    }

    @Test
    public void updatequery() {
        String oppdatertNavn = "oppdatert navn";
        getTestobjectWithId("007").toInsertQuery(db, TESTTABLE1).execute();
        SqlUtils.update(db, TESTTABLE1).set(NAVN, oppdatertNavn)
                .whereEquals(ID, "007").execute();

        Testobject retrieved = Testobject.getSelectQuery(ds, TESTTABLE1)
                .where(WhereClause.equals(ID, "007")).execute();

        assertThat(retrieved.getNavn()).isEqualTo(oppdatertNavn);
    }

    @Test
    public void upsertQuery() {
        String oppdatertNavn1 = "oppdatert navn1";
        String oppdatertNavn2 = "oppdatert navn2";

        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, oppdatertNavn1)
                .set(ID, "007")
                .where(WhereClause.equals(ID, "007"))
                .execute();

        Testobject retrieved1 = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute();
        assertThat(retrieved1.getNavn()).isEqualTo(oppdatertNavn1);

        SqlUtils.upsert(db, TESTTABLE1)
                .set(NAVN, oppdatertNavn2)
                .set(ID, "007")
                .where(WhereClause.equals(ID, "007"))
                .execute();

        Testobject retrieved2 = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute();
        assertThat(retrieved2.getNavn()).isEqualTo(oppdatertNavn2);
    }

    @Test
    public void updateBatchQuery() {
        String oppdatertNavn = "oppdatert navn";
        List<Testobject> objects = new ArrayList<>();
        objects.add(getTestobjectWithId("001"));
        objects.add(getTestobjectWithId("002"));
        objects.add(getTestobjectWithId("003"));
        objects.add(getTestobjectWithId("004"));
        objects.add(getTestobjectWithId("005"));
        objects.add(getTestobjectWithId("006"));
        objects.add(getTestobjectWithId("007"));

        Testobject.getInsertBatchQuery(db, TESTTABLE1)
                .execute(objects);

        UpdateBatchQuery<Testobject> updateBatchQuery =  new UpdateBatchQuery<>(db, TESTTABLE1);
        List<Testobject> updateObjects = new ArrayList<>();
        updateObjects.add(getTestobjectWithId("001").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("002").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("003").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("004").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("005").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("006").setNavn(oppdatertNavn));
        updateObjects.add(getTestobjectWithId("007").setNavn(oppdatertNavn));

        updateBatchQuery
                .add(NAVN, Testobject::getNavn, String.class)
                .addWhereClause(object -> WhereClause.equals(ID, object.getId())).execute(updateObjects);

        List<Testobject> retrieved = Testobject.getSelectQuery(ds, TESTTABLE1)
                .where(WhereIn.of(ID, asList("001", "002", "003", "004", "005", "006", "007"))).executeToList();

        assertThat(retrieved.stream().map(Testobject::getNavn).distinct().collect(Collectors.toList())).containsOnly(oppdatertNavn);
    }

    @Test
    public void deleteQuery() {
        getTestobjectWithId("007").toInsertQuery(db, TESTTABLE1).execute();
        assertThat(Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute()).isNotNull();

        SqlUtils.delete(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute();

        assertThat(Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereClause.equals(ID, "007")).execute());
    }

    @Test
    public void batchInsertAndSelect() {
        List<Testobject> objects = new ArrayList<>();
        objects.add(getTestobjectWithId("001"));
        objects.add(getTestobjectWithId("002"));
        objects.add(getTestobjectWithId("003"));
        objects.add(getTestobjectWithId("004"));
        objects.add(getTestobjectWithId("005"));
        objects.add(getTestobjectWithId("006"));
        objects.add(getTestobjectWithId("007"));

        Testobject.getInsertBatchQuery(db, TESTTABLE1)
                .execute(objects);

        List<Testobject> retrieved = SqlUtils.select(ds, TESTTABLE1, Testobject::mapper)
                .column(ID)
                .column(NAVN)
                .column(BIRTHDAY)
                .column(DEAD)
                .column(NUMBER_OF_PETS)
                .where(WhereIn.of(ID, asList("001", "002", "003", "004", "005", "006", "007")))
                .executeToList();

        assertThat(retrieved).isEqualTo(objects);
    }

    @Test
    public void leftJoinOn() {
        getTestobjectWithId("007").toInsertQuery(db, TESTTABLE1).execute();
        db.execute("INSERT INTO TESTTABLE2 (ID, ADDRESS) VALUES ('007', 'andeby')");
        Testobject retrieved = Testobject
                .getSelectWithAddressQuery(ds, TESTTABLE1)
                .leftJoinOn(TESTTABLE2, ID, ID)
                .where(WhereClause.equals(ID, "007"))
                .execute();

        assertThat(retrieved.getAddress()).isEqualTo("andeby");
    }

    @Test
    public void selectAll() {
        getTestobjectWithId("001").toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("003").toInsertQuery(db, TESTTABLE1).execute();
        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1).executeToList();

        assertThat(testobjects.size()).isEqualTo(3);
    }

    @Test
    public void orderByDesc() {
        getTestobjectWithId("001").setNumberOfPets(0).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").setNumberOfPets(5).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("003").setNumberOfPets(10).toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1)
                .orderBy(OrderClause.desc("NUMBER_OF_PETS"))
                .executeToList();

        assertThat(testobjects.size()).isEqualTo(3);
        assertThat(testobjects.get(0).numberOfPets).isEqualTo(10);
        assertThat(testobjects.get(0).id).isEqualTo("003");
    }

    @Test
    public void orderByAsc() {
        getTestobjectWithId("001").setNumberOfPets(10).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").setNumberOfPets(5).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("003").setNumberOfPets(0).toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1)
                .orderBy(OrderClause.asc("NUMBER_OF_PETS"))
                .executeToList();

        assertThat(testobjects.size()).isEqualTo(3);
        assertThat(testobjects.get(0).numberOfPets).isEqualTo(0);
        assertThat(testobjects.get(0).id).isEqualTo("003");
    }

    @Test
    public void orderAndWhere() {
        getTestobjectWithId("001").setDead(true).setNumberOfPets(0).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").setDead(true).setNumberOfPets(5).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("003").setDead(true).setNumberOfPets(10).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("004").setNumberOfPets(20).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("005").setNumberOfPets(25).toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1)
                .where(WhereClause.equals("DEAD", true))
                .orderBy(OrderClause.desc("NUMBER_OF_PETS"))
                .executeToList();

        assertThat(testobjects.size()).isEqualTo(3);
        assertThat(testobjects.get(0).numberOfPets).isEqualTo(10);
        assertThat(testobjects.get(0).id).isEqualTo("003");
    }

    @Test
    public void whereIsNotNull() {
        getTestobjectWithId("007").setBirthday(null).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("006").toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> birthdayNotNull = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereIsNotNull.of(BIRTHDAY)).executeToList();
        List<Testobject> birthdayNull = Testobject.getSelectQuery(ds, TESTTABLE1).where(WhereIsNull.of(BIRTHDAY)).executeToList();

        assertThat(birthdayNotNull.size()).isEqualTo(1);
        assertThat(birthdayNull.size()).isEqualTo(1);

        assertThat(birthdayNotNull.get(0).getBirthday()).isNotNull();
        assertThat(birthdayNull.get(0).getBirthday()).isNull();
    }

    @Test
    public void limit() {
        getTestobjectWithId("003").setDead(true).setNumberOfPets(2).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("004").setDead(true).setNumberOfPets(3).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("009").setDead(true).setNumberOfPets(8).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("007").setDead(true).setNumberOfPets(6).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").setDead(true).setNumberOfPets(1).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("008").setDead(true).setNumberOfPets(7).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("001").setDead(true).setNumberOfPets(0).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("006").setDead(true).setNumberOfPets(5).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("005").setDead(true).setNumberOfPets(4).toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1)
                .orderBy(OrderClause.asc(NUMBER_OF_PETS))
                .limit(5)
                .executeToList();

        assertThat(testobjects.stream()
                .map(Testobject::getNumberOfPets).collect(Collectors.toList())).isEqualTo(asList(0,1,2,3,4));
    }

    @Test
    public void limitWithOffset() {
        getTestobjectWithId("003").setDead(true).setNumberOfPets(2).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("004").setDead(true).setNumberOfPets(3).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("009").setDead(true).setNumberOfPets(8).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("007").setDead(true).setNumberOfPets(6).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("002").setDead(true).setNumberOfPets(1).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("008").setDead(true).setNumberOfPets(7).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("001").setDead(true).setNumberOfPets(0).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("006").setDead(true).setNumberOfPets(5).toInsertQuery(db, TESTTABLE1).execute();
        getTestobjectWithId("005").setDead(true).setNumberOfPets(4).toInsertQuery(db, TESTTABLE1).execute();

        List<Testobject> testobjects = Testobject.getSelectQuery(ds, TESTTABLE1)
                .orderBy(OrderClause.asc(NUMBER_OF_PETS))
                .limit(2, 5)
                .executeToList();

        assertThat(testobjects.stream()
                .map(Testobject::getNumberOfPets).collect(Collectors.toList())).isEqualTo(asList(2,3,4,5,6));
    }

    private Testobject getTestobjectWithId(String id) {
        return new Testobject()
                .setNavn("navn navnesen")
                .setId(id)
                .setBirthday(new Timestamp(0))
                .setNumberOfPets(4)
                .setDead(false);
    }
}