package no.nav.dialogarena.config.fasit;

@SuppressWarnings("unused")
public enum TestEnvironment {

    T1("t1"),
    T2("t2"),
    T3("t3"),
    T4("t4"),
    T5("t5"),
    T6("t6"),
    T7("t7"),
    T8("t8"),
    T9("t9"),
    T10("t10"),
    T11("t11"),
    T12("t12"),
    TA("ta"),
    TX("tx"),
    TY("ty"),

    Q4("q4"),
    Q6("q6"),
    Q0("q0")
    ;

    private final String env;

    TestEnvironment(final String env) {
        this.env = env;
    }

    @Override
    public String toString() {
        return env;
    }

    public boolean matcher(String environment) {
        return env.equalsIgnoreCase(environment);
    }

}
