package no.nav.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilsTest {

    @Test
    public void linearInterpolationShouldReturnCorrectValues() {
        assertEquals(10, MathUtils.linearInterpolation(10, 20, 0F));
        assertEquals(20, MathUtils.linearInterpolation(10, 20, 1F));
        assertEquals(15, MathUtils.linearInterpolation(10, 20, 0.5F));

        assertEquals(10, MathUtils.linearInterpolation(10, 20, -0.1F));
        assertEquals(20, MathUtils.linearInterpolation(10, 20, 1.1F));
    }

}
