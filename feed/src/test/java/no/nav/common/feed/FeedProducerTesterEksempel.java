package no.nav.common.feed;


import no.nav.common.feed.common.FeedElement;
import no.nav.common.feed.controller.FeedController;
import no.nav.common.feed.producer.FeedProducer;
import no.nav.common.feed.producer.FeedProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FeedProducerTesterEksempel implements FeedProducerTester, FeedProvider<Integer> {

    @Override
    public FeedController getFeedController() {
        return new FeedController().addFeed(
                "test", FeedProducer.<Integer>builder()
                        .provider(this)
                        .build()
        );
    }

    @Override
    public void opprettElementForFeed(String feedName, String id) {
        tall.add(Integer.parseInt(id));
    }

    @Override
    public String unikId(String feedName) {
        return Integer.toString(nesteTall++);
    }

    @Override
    public String forsteMuligeId(String feedName) {
        return Integer.toString(0);
    }

    @Override
    public Stream<FeedElement<Integer>> fetchData(String id, int pageSize) {
        return provider.fetchData(id, pageSize);
    }

    // viser at FeedProducerTester feiler hvis man har gjort feil i implementasjonen
    @Test
    public void feilerVedManglendePaging() {
        provider = provider((stream, fra, pageSize) -> stream
                .filter(i -> i >= fra)
                .sorted(Integer::compareTo)
//                .limit(pageSize)
        );
        assertThatThrownBy(this::alleProdusenterTarHensynTilPageSize).isInstanceOf(AssertionError.class);
    }

    @Test
    public void feilerVedManglendeProgresjon() {
        provider = provider((stream, fra, pageSize) -> stream
//                .filter(i -> i >= fra)
                .sorted(Integer::compareTo)
                .limit(pageSize)
        );
        assertThatThrownBy(this::nesteIdBlirForsteINesteRespons).isInstanceOf(AssertionError.class);
    }

    @Test
    public void feilerVedManglendeSortering() {
        provider = provider((stream, fra, pageSize) -> stream
                .filter(i -> i >= fra)
//                .sorted(Integer::compareTo)
                .limit(pageSize)
        );
        assertThatThrownBy(this::alleProdusenterHenterISortertRekkefolge).isInstanceOf(AssertionError.class);
    }

    @Test
    public void feilerVedSorteringEtterLimiting() {
        provider = provider((stream, fra, pageSize) -> stream
                // hvis man f.eks. bruker rownum + order by i SQL blir evalueringsrekkefølgen:
                .filter(i -> i >= fra) // e.g WHERE
                .limit(pageSize) // e.g rownum <= pageSize
                .sorted(Integer::compareTo) // e.g ORDER BY
        );
        assertThatThrownBy(this::alleProdusenterHenterISortertRekkefolge).isInstanceOf(AssertionError.class);
    }

    // Provider-implementasjon
    private static int nesteTall;

    private List<Integer> tall = new ArrayList<>();
    private FeedProvider<Integer> provider = provider(this::korrektPaginering);

    private FeedProvider<Integer> provider(Paginering paginering) {
        return (id, pageSize) -> paginering.paginer(
                tall.stream(),
                Integer.parseInt(id),
                pageSize
        ).map(this::feedElement);
    }

    private Stream<Integer> korrektPaginering(Stream<Integer> stream, int fra, int pageSize) {
        return stream
                .filter(i -> i >= fra)
                .sorted(Integer::compareTo)
                .limit(pageSize);
    }

    private FeedElement<Integer> feedElement(Integer integer) {
        return new FeedElement<Integer>().setId(Integer.toString(integer)).setElement(integer);
    }

    interface Paginering {
        Stream<Integer> paginer(Stream<Integer> stream, int offset, int pageSize);
    }

}
