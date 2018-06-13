package no.nav.validation;


import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidationUtilsTest {

    @Test
    public void smoketest() {
        ValidationUtils.validate(new OldPerson()
                .setName("Per")
                .setAge(84)
                .setHobby("programming")
                .setRetired(true)
                .setSsn("12345678901")
        );

        assertThatThrownBy(() -> ValidationUtils.validate(new OldPerson()))
                .hasMessage("" +
                        "Validation of 'ValidationUtilsTest.OldPerson(name=null, age=0, hobby=null, retired=false, ssn=)' failed:\n" +
                        "age = 0 :: must be greater than or equal to 67\n" +
                        "hobby = null :: must have hobby\n" +
                        "name = null :: must not be empty\n" +
                        "retired = false :: must be true\n" +
                        "ssn =  :: must match \"\\d+\"" +
                        ""
                );
    }

    @Data
    @Accessors(chain = true)
    private static class OldPerson {

        @NotEmpty
        private String name;

        @Min(67)
        public int age;

        @NotEmpty(message = "must have hobby")
        public String hobby;

        @AssertTrue
        public boolean retired;

        @Pattern(regexp = "\\d+")
        public String ssn = "";

    }

}