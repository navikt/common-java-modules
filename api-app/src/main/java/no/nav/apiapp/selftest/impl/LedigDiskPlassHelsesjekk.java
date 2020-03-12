package no.nav.apiapp.selftest.impl;

import no.nav.common.health.Helsesjekk;
import no.nav.common.health.HelsesjekkMetadata;

import java.io.File;


public class LedigDiskPlassHelsesjekk implements Helsesjekk {

    private static final long LIMIT = 300_000_000L;
    private File absoluteFile = new File(".").getAbsoluteFile();

    @Override
    public void helsesjekk() {
        if (absoluteFile.getFreeSpace() < LIMIT) {
            throw new IllegalStateException(String.format("Mindre enn %s MB ledig diskplass for %s",
                    LIMIT / 1_000_000,
                    absoluteFile
            ));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "free_disk",
                String.format("Diskplass for: %s", absoluteFile.getAbsolutePath()),
                String.format("Sjekk for om det er mindre enn %s MB displass ledig", LIMIT / 1_000_000),
                false
        );
    }

}
