package no.nav.common.health.generators;

import lombok.Builder;
import no.nav.common.health.SelfTestBaseServlet;
import no.nav.common.health.domain.Selftest;
import no.nav.common.health.domain.SelftestResult;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.json.JsonUtils.toJson;

public class SelftestJsonGenerator {

    public static String generate(Selftest selftest) {
        return toJson(toDTO(selftest));
    }

    private static SelftestDTO toDTO(Selftest selftest) {
        return SelftestDTO.builder()
                .application(selftest.getApplication())
                .version(selftest.getVersion())
                .timestamp(selftest.getTimestamp())
                .aggregateResult(SelfTestBaseServlet.statusToCode(selftest.getAggregateResult()))
                .checks(selftest.getChecks().stream().map(SelftestJsonGenerator::toDTO).collect(toList()))
                .build();
    }

    private static SelftestEndpointDTO toDTO(SelftestResult selftestResult) {
        return SelftestEndpointDTO.builder()
                .endpoint(selftestResult.getEndpoint())
                .description(selftestResult.getDescription())
                .errorMessage(selftestResult.getErrorMessage())
                .result(SelfTestBaseServlet.statusToCode(selftestResult.getResult()))
                .responseTime(Long.toString(selftestResult.getResponseTime()))
                .stacktrace(selftestResult.getStacktrace())
                .critical(selftestResult.isCritical())
                .build();
    }

    @Builder
    private static class SelftestDTO {
        private String application;
        private String version;
        private String timestamp;
        private int aggregateResult;
        private List<SelftestEndpointDTO> checks;
    }

    @Builder
    private static class SelftestEndpointDTO {
        private String endpoint;
        private String description;
        private String errorMessage;
        private int result;
        private String responseTime;
        private String stacktrace;
        private boolean critical;
    }

}
