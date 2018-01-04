package no.nav.sbl.sql.value;

import no.nav.sbl.sql.DbConstants;

public class DbConstantValue extends Value<String> {
    public DbConstantValue(DbConstants value) {
        super(value.sql);
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