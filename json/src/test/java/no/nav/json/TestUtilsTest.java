package no.nav.json;

import org.assertj.core.api.ThrowableAssert;
import org.junit.ComparisonFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static no.nav.json.TestUtils.assertEqualJson;
import static no.nav.json.TestUtils.assertEqualJsonArray;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TestUtilsTest {

    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";
    private static final String TEST_OBJECT_JSON = JsonUtils.toJson(new JsonUtilsTest.TestObject());
    private static final String TEST_OBJECT_IN_ARRAY_JSON = "[" + TEST_OBJECT_JSON + "]";

    @Nested
    class assertEqualJson {
        @Test
        public void equal() {
            assertEqualJson(EMPTY_OBJECT, EMPTY_OBJECT);
            assertEqualJson(TEST_OBJECT_JSON, TEST_OBJECT_JSON);
        }

        @Test
        public void unequal() {
            comparisonFailure(() -> assertEqualJson(EMPTY_OBJECT, TEST_OBJECT_JSON));
        }

    }

    @Nested
    class assertEqualJsonArray {

        @Test
        public void equal() {
            assertEqualJsonArray(EMPTY_ARRAY, EMPTY_ARRAY);
            assertEqualJsonArray(TEST_OBJECT_IN_ARRAY_JSON, TEST_OBJECT_IN_ARRAY_JSON);
        }

        @Test
        public void unequal() {
            comparisonFailure(() -> assertEqualJsonArray(EMPTY_ARRAY, "[1,2,3]"));
        }

    }

    private void comparisonFailure(ThrowableAssert.ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable).isInstanceOf(ComparisonFailure.class);
    }

}