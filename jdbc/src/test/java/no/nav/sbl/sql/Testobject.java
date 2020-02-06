package no.nav.sbl.sql;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static no.nav.sbl.sql.SqlUtilsTest.*;


@Data
@Accessors(chain = true)
public class Testobject {
    String navn;
    String address;
    String bankName;
    String id;
    Timestamp birthday;
    boolean dead;
    int numberOfPets;

    public static Testobject mapper(ResultSet rs) throws SQLException {
        return new Testobject()
                .setBirthday(rs.getTimestamp(BIRTHDAY))
                .setDead(rs.getBoolean(DEAD))
                .setId(rs.getString(ID))
                .setNavn(rs.getString(NAVN))
                .setNumberOfPets(rs.getInt(NUMBER_OF_PETS));
    }

    @SneakyThrows
    public static Testobject mapperWithAddressAndBank(ResultSet rs) {
        return new Testobject()
                .setBirthday(rs.getTimestamp(BIRTHDAY))
                .setDead(rs.getBoolean(DEAD))
                .setId(rs.getString(ID))
                .setNavn(rs.getString(NAVN))
                .setNumberOfPets(rs.getInt(NUMBER_OF_PETS))
                .setAddress(rs.getString(ADDRESS))
                .setBankName(rs.getString(BANK_NAME));
    }

    public static SelectQuery<Testobject> getSelectWithAddressAndBankQuery(JdbcTemplate db, String table) {
        return SqlUtils.select(db, table, Testobject::mapperWithAddressAndBank)
                .column(BIRTHDAY)
                .column(DEAD)
                .column(ID)
                .column(NAVN)
                .column(NUMBER_OF_PETS)
                .column(ADDRESS)
                .column(BANK_NAME);
    }

    public static SelectQuery<Testobject> getSelectQuery(JdbcTemplate db, String table) {
        return SqlUtils.select(db, table, Testobject::mapper)
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
