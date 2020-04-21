package no.nav.common.test.coverage;

import org.junit.Test;



public class ExcerciseTest {

    private static final Excercise EXCERCISE = new Excercise(ExcerciseTest.class.getPackage().getName());


    @Test
    public void runEnumExcercise() {
        EXCERCISE.generatedStaticValueOfMethodInEnums();
    }

    @Test
    public void runUtilClassInstantiation() {
        EXCERCISE.privateDefaultConstructorsInStaticUtilClasses();
    }
}
