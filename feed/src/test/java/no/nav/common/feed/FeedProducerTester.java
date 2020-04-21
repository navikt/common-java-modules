package no.nav.common.feed;


import no.nav.common.feed.common.FeedElement;
import no.nav.common.feed.common.FeedResponse;
import no.nav.common.feed.controller.FeedController;
import no.nav.common.feed.producer.FeedProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.shuffle;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Bruk denne til å teste at producere er implementert korrekt
 */
public interface FeedProducerTester {

    FeedController getFeedController();

    void opprettElementForFeed(String feedName, String id);

    String unikId(String feedName);

    String forsteMuligeId(String feedName);

    @Test
    default void kanHenteElementerFraAlleProdusenter() {
        FeedController feedController = getFeedController();
        feedController.getFeeds().forEach(feedName -> {
            String tilfeldigId = unikId(feedName);
            opprettElementForFeed(feedName, tilfeldigId);
            List<? extends FeedElement<?>> elements = feedController.getFeeddata(feedName, forsteMuligeId(feedName), null).getElements();
            assertThat(elements).hasSize(1);
            assertThat(elements.get(0).getId()).isEqualTo(tilfeldigId);
        });
    }

    @Test
    default void nesteIdBlirForsteINesteRespons() {
        FeedController feedController = getFeedController();
        feedController.getFeeds().forEach(feedName -> {
            range(0, 10).forEach(i -> opprettElementForFeed(feedName, unikId(feedName)));
            String nextPageId = feedController.getFeeddata(feedName, forsteMuligeId(feedName), null).getNextPageId();
            FeedElement<?> nesteElement = feedController.getFeeddata(feedName, nextPageId, null).getElements().get(0);
            assertThat(nextPageId).isEqualTo(nesteElement.getId());
        });
    }

    @Test
    default void alleProdusenterTarHensynTilPageSize() {
        getFeedController().getFeeds().forEach(feedName -> {
            int pageSize = 5;
            range(0, 3 * pageSize).forEach(i -> opprettElementForFeed(feedName, unikId(feedName)));
            String nextPageId = getFeedController().getFeeddata(feedName, forsteMuligeId(feedName), pageSize).getNextPageId();

            // henter noe midt i feeden
            FeedResponse<?> feedResponse = getFeedController().getFeeddata(feedName, nextPageId, pageSize);
            assertThat(feedResponse.getElements().size())
                    .as("%s for feed '%s' fetcher for mye data", FeedProvider.class.getSimpleName(), feedName)
                    .isLessThanOrEqualTo(pageSize)
                    .as("%s for feed '%s' fetcher for lite data", FeedProvider.class.getSimpleName(), feedName)
                    .isGreaterThanOrEqualTo(pageSize);
        });
    }

    // Det er lett å gå på en smell hvis man både skal sortere og limite i SQL.
    // rownum + order by er f.eks. ikke korrekt
    @Test
    default void alleProdusenterHenterISortertRekkefolge() {
        FeedController feedController = getFeedController();
        feedController.getFeeds().forEach(feedName -> {
            int antallElementer = 100;

            // opprett elementer i tilfeldig rekkefølge
            List<String> iderITilfeldigRekkefolge = range(0, antallElementer)
                    .mapToObj(i -> unikId(feedName))
                    .collect(toList());
            shuffle(iderITilfeldigRekkefolge);

            assertThat(iderITilfeldigRekkefolge)
                    .describedAs("unikId() produserer ikke unike ider")
                    .hasSameSizeAs(new HashSet<>(iderITilfeldigRekkefolge));
            iderITilfeldigRekkefolge.forEach(id -> opprettElementForFeed(feedName, id));

            // les gjennom hele feeden gradvis
            List<FeedElement<?>> elementer = new ArrayList<>();
            String id = forsteMuligeId(feedName);
            List<? extends FeedElement<?>> elements;
            do {
                FeedResponse<?> feedResponse = feedController.getFeeddata(feedName, id, 5);
                elements = feedResponse.getElements();
                elements.forEach(elementer::add);
                id = feedResponse.getNextPageId();
            } while (elements.size() > 1);

            // Skal egentlig ikke inntreffe siden FeedControllen uansett sorterer elementene
            assertThat(elementer).isSorted();

            assertThat(elementer.stream().map(FeedElement::getId))
                    .describedAs("mangler elementer ved gjennomlesning av feed '%s'. Kontroller at feed-produceren sorterer og paginerer korrekt", feedName)
                    .contains(iderITilfeldigRekkefolge.toArray(new String[0]));
        });
    }

}
