package no.nav.apiapp.db;

import lombok.SneakyThrows;
import no.nav.sbl.jdbc.Database;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/db")
public class DatabaseExample {

    private final Database database;

    public DatabaseExample(Database database) {
        this.database = database;
    }

    @POST
    public List<Map<String, Object>> query(String sql) {
        return database.query(sql, this::map);
    }

    @SneakyThrows
    private Map<String, Object> map(ResultSet resultSet) {
        HashMap<String, Object> result = new HashMap<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            result.put(metaData.getColumnName(i), resultSet.getObject(i));
        }
        return result;
    }

}
