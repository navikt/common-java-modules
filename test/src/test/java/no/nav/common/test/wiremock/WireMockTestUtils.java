package no.nav.common.test.wiremock;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

public final class WireMockTestUtils {
    private WireMockTestUtils() {}

    public static WireMockExtension createWireMockExtension() {
        return WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();
    }
}
