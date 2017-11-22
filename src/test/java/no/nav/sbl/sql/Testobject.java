package no.nav.sbl.sql;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static no.nav.sbl.sql.SqlUtilsTest.*;


@Data
@Accessors(chain = true)
public class Testobject {
    String navn;
    String address;
    String id;
    Timestamp birthday;
    boolean dead;
    int numberOfPets;

    @SneakyThrows
    public static Testobject mapper(ResultSet rs) {
        return new Testobject()
                .setBirthday(rs.getTimestamp(BIRTHDAY))
                .setDead(rs.getBoolean(DEAD))
                .setId(rs.getString(ID))
                .setNavn(rs.getString(NAVN))
                .setNumberOfPets(rs.getInt(NUMBER_OF_PETS));
    }

    @SneakyThrows
    public static Testobject mapperWithAddress(ResultSet rs) {
        return new Testobject()
                .setBirthday(rs.getTimestamp(BIRTHDAY))
                .setDead(rs.getBoolean(DEAD))
                .setId(rs.getString(ID))
                .setNavn(rs.getString(NAVN))
                .setNumberOfPets(rs.getInt(NUMBER_OF_PETS))
                .setAddress(rs.getString(ADDRESS));
    }

    public static SelectQuery<Testobject> getSelectWithAddressQuery(DataSource ds, String table) {
        return SqlUtils.select(ds, table, Testobject::mapperWithAddress)
                .column(BIRTHDAY)
                .column(DEAD)
                .column(ID)
                .column(NAVN)
                .column(NUMBER_OF_PETS)
                .column(ADDRESS);
    }

    public static SelectQuery<Testobject> getSelectQuery(DataSource ds, String table) {
        return SqlUtils.select(ds, table, Testobject::mapper)
                .column(BIRTHDAY)
                .column(DEAD)
                .column(ID)
                .column(NAVN)
                .column(NUMBER_OF_PETS);
    }

    public InsertQuery toInsertQuery(JdbcTemplate db, String table) {
        return SqlUtils.insert(db, table)
                .value(BIRTHDAY, birthday)
                .value(ID, id)
                .value(DEAD, dead)
                .value(NUMBER_OF_PETS, numberOfPets)
                .value(NAVN, navn);
    }

    public static InsertBatchQuery<Testobject> getInsertBatchQuery(JdbcTemplate db, String table) {
        InsertBatchQuery<Testobject> insertBatchQuery = new InsertBatchQuery<>(db, table);
        return insertBatchQuery
                .add(NAVN, Testobject::getNavn, String.class)
                .add(DEAD, Testobject::isDead, Boolean.class)
                .add(ID, Testobject::getId, String.class)
                .add(BIRTHDAY, Testobject::getBirthday, Timestamp.class)
                .add(NUMBER_OF_PETS, Testobject::getNumberOfPets, Integer.class);
    }
}
