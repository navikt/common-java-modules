package no.nav.sbl.sql.mapping;

import io.vavr.CheckedFunction2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;

class ValueMapping {
    private static Map<Class<?>, CheckedFunction2<ResultSet, String, ?>> valuemappers = HashMap.empty();

    static {
        valuemappers = valuemappers.put(String.class, ResultSet::getString);
        valuemappers = valuemappers.put(Boolean.class, ResultSet::getBoolean);
        valuemappers = valuemappers.put(Integer.class, ResultSet::getInt);
        valuemappers = valuemappers.put(Byte.class, ResultSet::getByte);
        valuemappers = valuemappers.put(Byte[].class, ResultSet::getBytes);
        valuemappers = valuemappers.put(Double.class, ResultSet::getDouble);
        valuemappers = valuemappers.put(Float.class, ResultSet::getFloat);
        valuemappers = valuemappers.put(Long.class, ResultSet::getLong);
        valuemappers = valuemappers.put(Short.class, ResultSet::getShort);
        valuemappers = valuemappers.put(Object.class, ResultSet::getObject);
        valuemappers = valuemappers.put(BigDecimal.class, ResultSet::getBigDecimal);
        valuemappers = valuemappers.put(Time.class, ResultSet::getTime);
        valuemappers = valuemappers.put(Timestamp.class, ResultSet::getTimestamp);
        valuemappers = valuemappers.put(Date.class, ResultSet::getDate);
        valuemappers = valuemappers.put(URL.class, ResultSet::getURL);
        valuemappers = valuemappers.put(Blob.class, ResultSet::getBlob);
        valuemappers = valuemappers.put(Clob.class, ResultSet::getBlob);
    }

    @SuppressWarnings("unchecked")
    static <FROM> FROM getValue(QueryMapping.InternalColumn<FROM, ?> column, ResultSet rs) {
        return (FROM) valuemappers
                .get(column.from)
                .toTry(() -> new RuntimeException("Mapping from " + column.from + " not found."))
                .mapTry((func) -> func.apply(rs, column.name))
                .get();
    }
}
