package no.nav.common.cxf;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InstanceSwitcherTest {

    private Properties originalProperties;

    @BeforeEach
    public void setUp() {
        originalProperties = (Properties) System.getProperties().clone();
        clearProperty("mock");
        clearProperty("mock.simulate.error");
        clearProperty("tillatmock");
    }

    @AfterEach
    public void tearDown() {
        System.setProperties(originalProperties);
    }

    @Test
    public void normalUtforelse() {
        String nokkel = "mock";
        setProperty(nokkel, "false");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThat(switcher.doSomething(), is("originalklasse"));
    }

    @Test
    public void normalMockSkalKjoreNormalt() {
        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "true");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThat(switcher.doSomething(), is("mockklasse"));
    }

    @Test
    public void normalMockSkalSimulereFeil() {
        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "true");
        setProperty(nokkel + ".simulate.error", "true");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThrows(RuntimeException.class, switcher::doSomething);
    }

    @Test
    public void skalSimulereInvocationTargetException() throws InvocationTargetException {
        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "false");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThrows(InvocationTargetException.class, switcher::simulererInvokationTargetException);
    }

    private ClassV1 createClassV1() {
        return new ClassV1() {
            @Override
            public String doSomething() {
                return "originalklasse";
            }

            @Override
            public void simulererInvokationTargetException() throws InvocationTargetException {
                throw new InvocationTargetException(new Throwable("message"));
            }
        };
    }

    private ClassV1 createClassV1Mock() {
        return new ClassV1() {
            @Override
            public String doSomething() {
                return "mockklasse";
            }

            @Override
            public void simulererInvokationTargetException() {}
        };
    }

    private interface ClassV1 {
        String doSomething();

        void simulererInvokationTargetException() throws InvocationTargetException;
    }
}
