package no.nav.common.cxf;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InstanceSwitcherTest {

    @Test
    public void normalUtforelse() throws InvocationTargetException {
        String nokkel = "mock";
        setProperty(nokkel, "false");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThat(switcher.doSomething(), is("originalklasse"));

        clearProperty(nokkel);
    }

    @Test
    public void normalMockSkalKjoreNormalt() throws InvocationTargetException {
        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "true");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        assertThat(switcher.doSomething(), is("mockklasse"));

        clearProperty(nokkel);
    }

    @Test(expected = RuntimeException.class)
    public void normalMockSkalSimulereFeil() throws InvocationTargetException {

        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "true");
        setProperty(nokkel + ".simulate.error", "true");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        switcher.doSomething();

        clearProperty(nokkel);
        clearProperty(nokkel + ".simulate.error");
    }

    @Test(expected = InvocationTargetException.class)
    public void skalSimulereInvocationTargetException() throws InvocationTargetException {

        String nokkel = "mock";

        setProperty("tillatmock", "true");
        setProperty(nokkel, "false");

        ClassV1 test = createClassV1();
        ClassV1 mock = createClassV1Mock();
        ClassV1 switcher = InstanceSwitcher.createSwitcher(test, mock, nokkel, ClassV1.class);

        switcher.simulererInvokationTargetException();

        clearProperty(nokkel);
    }

    private  ClassV1 createClassV1(){
        return new ClassV1() {
            @Override
            public String doSomething(){
                return "originalklasse";
            }

            @Override
            public void simulererInvokationTargetException() throws InvocationTargetException {
                throw new InvocationTargetException(new Throwable("message"));
            }

            @Override
            public void simulererIllegalAccessException() throws IllegalAccessException {
                throw new IllegalAccessException("message");
            }
        };
    }

    private ClassV1 createClassV1Mock(){
        return new ClassV1() {
            @Override
            public String doSomething() {
                return "mockklasse";
            }

            @Override
            public void simulererInvokationTargetException() {

            }

            @Override
            public void simulererIllegalAccessException() {

            }
        };
    }

    private interface ClassV1 {
        String doSomething();
        void simulererInvokationTargetException() throws InvocationTargetException;
        void simulererIllegalAccessException() throws IllegalAccessException;
    }
}