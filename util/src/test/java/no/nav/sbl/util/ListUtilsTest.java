package no.nav.sbl.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ListUtilsTest {

    @Test
    public void mutableList_smoketest() {
        assertThat(ListUtils.mutableList()).isEmpty();
        assertThat(ListUtils.mutableList(1, 2, 3)).isEqualTo(Arrays.asList(1, 2, 3));
        List<Integer> integers = ListUtils.mutableList(1, 2, 3);
        integers.add(4);
        assertThat(integers).isEqualTo(Arrays.asList(1, 2, 3, 4));
    }

}