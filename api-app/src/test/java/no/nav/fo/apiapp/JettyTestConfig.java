package no.nav.fo.apiapp;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JettyTestConfig {
    boolean allowClientStorage;
    boolean disablePragmaHeader;
}