package no.nav.common.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;

public class CollectionUtilsTest {
    @Test
    public void should_partition_list() {
        int size = 1024;
        ArrayList<Object> list = new ArrayList<>(size);
        IntStream.range(0, size).forEach(list::add);

        List<List<Object>> partitionedList = CollectionUtils.partition(list, 100);
        assertThat(partitionedList.size()).isEqualTo(11);
    }
}
