package no.nav.apiapp.version;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Version {
    public final String component;
    public final String version;
}
