package no.nav.common.kafka.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.common.json.JsonUtils;
import no.nav.common.utils.StringUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KafkaUtils {

    public static String headersToJson(Headers headers) {
        if (headers == null) {
            return "[]";
        }

        List<KafkaHeader> kafkaHeaders = Arrays.stream(headers.toArray())
                .map(header -> new KafkaHeader(header.key(), header.value()))
                .collect(Collectors.toList());

        return JsonUtils.toJson(kafkaHeaders);
    }

    public static Headers jsonToHeaders(String headersJson) {
        if (StringUtils.nullOrEmpty(headersJson)) {
            return new RecordHeaders();
        }

        List<Header> headers = JsonUtils.fromJsonArray(headersJson, KafkaHeader.class)
                .stream()
                .map(header -> new RecordHeader(header.getKey(), header.getValue()))
                .collect(Collectors.toList());

        return new RecordHeaders(headers);
    }

    /**
     * {@link org.apache.kafka.common.header.Header} contains information we dont need when serializing
     */
    @AllArgsConstructor
    @Data
    static class KafkaHeader {
        String key;
        byte[] value;
    }
}
