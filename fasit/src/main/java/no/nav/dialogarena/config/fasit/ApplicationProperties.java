package no.nav.dialogarena.config.fasit;

import lombok.Builder;

import java.util.Properties;

@Builder
public class ApplicationProperties extends Properties implements Scoped {

    private final String environment;
    private final String environmentClass;

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public String getEnvironmentClass() {
        return environmentClass;
    }

}
