package no.nav.sbl.sql;

public abstract class Value<T> {
    public final T sql;

    Value(T sql) {
        this.sql = sql;
    }

    public Object getSql() {
        return this.sql;
    }

    public abstract boolean hasPlaceholder();

    public abstract String getValuePlaceholder();

    static class DbConstantValue extends Value<String> {
        DbConstantValue(DbConstants value) {
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

    static class ObjectValue extends Value<Object> {
        ObjectValue(Object value) {
            super(value);
        }

        @Override
        public boolean hasPlaceholder() {
            return true;
        }

        @Override
        public String getValuePlaceholder() {
            return "?";
        }
    }

    public static Value of(Object value) {
        return new ObjectValue(value);
    }
    public static Value of(DbConstants value) {
        return new DbConstantValue(value);
    }
}
