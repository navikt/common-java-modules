package no.nav.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;

public class CollectionUtilsTest {
    @Test
    public void skal_partisjonere_med_riktig_innhold_storrelse_og_rekkefolge() {
        int size = 1024;
        List<Integer> list = IntStream.range(0, size).boxed().toList();

        List<List<Integer>> partitionedList = CollectionUtils.partition(list, 100);
        assertThat(partitionedList).hasSize(11);
        for (int i = 0; i < partitionedList.size(); i++) {
            int start = i * 100;
            List<Integer> expected =
                    IntStream.range(start, Math.min(start + 100, size)).boxed().toList();
            assertThat(partitionedList.get(i)).containsExactlyElementsOf(expected);
        }
        assertThat(partitionedList.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(list);
    }

    @Test
    public void skal_returnere_ingen_partisjoner_for_tom_liste() {
        assertThat(CollectionUtils.partition(List.of(), 100)).isEmpty();
    }

    @Test
    public void skal_partisjonere_uten_tom_rest_ved_eksakt_partisjonsstorrelse() {
        assertThat(CollectionUtils.partition(List.of(1, 2, 3, 4), 2)).containsExactly(List.of(1, 2), List.of(3, 4));
    }

    @Test
    public void skal_beholde_hele_listen_nar_partisjonsstorrelse_er_storre_enn_listen() {
        assertThat(CollectionUtils.partition(List.of(1, 2, 3), 100)).containsExactly(List.of(1, 2, 3));
    }
}
