package no.nav.sbl.sql.value;

import no.nav.sbl.sql.DbConstants;

public class ConstantValue extends Value<String> {
    public ConstantValue(DbConstants value) {
        super(value.sql);
    }
    public ConstantValue(String sql) {
        super(sql);
    }

    @Override
    public boolean hasPlaceholder() {
        return false;
    }

    @Override
    public String getValuePlaceholder() {
        return this.sql;
    }
}