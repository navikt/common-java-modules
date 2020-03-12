package no.nav.common.health;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.domain.Selftest;
import no.nav.common.health.domain.SelftestResult;
import no.nav.common.health.domain.Pingable;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Slf4j
public class SelfTestService {

    private final List<Pingable> pingables;

    private volatile List<SelftestResult> lastResult;
    private volatile long lastResultTime;

    public SelfTestService(Collection<? extends Pingable> pingables) {
        this.pingables = new ArrayList<>(pingables);
    }

    private SelfTestStatus getAggregertStatus() {
        boolean harKritiskFeil = lastResult.stream().anyMatch(SelfTestService::harKritiskFeil);
        boolean harFeil = lastResult.stream().anyMatch(SelftestResult::harFeil);

        if (harKritiskFeil) {
            return SelfTestStatus.ERROR;
        } else if (harFeil) {
            return SelfTestStatus.WARNING;
        }
        return SelfTestStatus.OK;
    }

    private static SelftestResult doPing(Pingable pingable) {
        long startTime = System.currentTimeMillis();
        Pingable.Ping ping = performPing(pingable);
        long responseTime = System.currentTimeMillis() - startTime;
        Pingable.Ping.PingMetadata metadata = ofNullable(ping.getMetadata()).orElseGet(SelfTestService::unknownMetadata);
        if (!ping.erVellykket() && !ping.erAvskrudd()) {
            log.warn("Feil ved SelfTest av " + metadata.getEndepunkt(), ping.getFeil());
        }
        return SelftestResult.builder()
                .id(metadata.getId())
                .responseTime(responseTime)
                .endpoint(metadata.getEndepunkt())
                .description(metadata.getBeskrivelse())
                .errorMessage(ping.getFeilmelding())
                .critical(metadata.isKritisk())
                .result(computePingResult(ping))
                .stacktrace(ofNullable(ping.getFeil())
                        .map(ExceptionUtils::getStackTrace)
                        .orElse(null)
                )
                .build();
    }

    private static Pingable.Ping performPing(Pingable pingable) {
        try {
            return ofNullable(pingable.ping()).orElseGet(() -> unknownPingResult(pingable));
        } catch (Exception e) {
            String className = pingable.getClass().getName();
            return Pingable.Ping.feilet(new Pingable.Ping.PingMetadata(className, className, className, true), e);
        }
    }

    private static Pingable.Ping unknownPingResult(Pingable pingable) {
        return Pingable.Ping.feilet(unknownMetadata(), new IllegalStateException(pingable.toString()));
    }

    private static Pingable.Ping.PingMetadata unknownMetadata() {
        return new Pingable.Ping.PingMetadata("unknown_ping", "?", "?", true);
    }

    public Selftest selfTest() {
        long requestTime = System.currentTimeMillis();
        // Beskytter pingables mot mange samtidige/tette requester.
        // Særlig viktig hvis det tar lang tid å utføre alle pingables
        synchronized (this) {
            if (requestTime > lastResultTime) {
                lastResult = pingables.stream().map(SelfTestService::doPing).collect(toList());
                lastResultTime = System.currentTimeMillis();
            }
        }

        return Selftest.builder()
                .application(EnvironmentUtils.getApplicationName().orElse("?"))
                .version(EnvironmentUtils.getApplicationVersion().orElse("?"))
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .aggregateResult(getAggregertStatus())
                .checks(lastResult)
                .build();
    }

    private static SelfTestStatus computePingResult(Pingable.Ping ping) {
        if (ping.erAvskrudd()) {
            return SelfTestStatus.DISABLED;
        } else {
            return ping.harFeil() ? SelfTestStatus.ERROR : SelfTestStatus.OK;
        }
    }

    private static boolean harKritiskFeil(SelftestResult selftestResult) {
        return selftestResult.harFeil() && selftestResult.isCritical();
    }

}
