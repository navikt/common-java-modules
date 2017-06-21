package no.nav.apiapp.selftest.impl;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;

import java.io.File;


public class LedigDiskPlassHelsesjekk implements Helsesjekk {

    private static final long LIMIT = 300_000_000L;
    private File absoluteFile = new File(".").getAbsoluteFile();

    @Override
    public void helsesjekk() {
        throw new IllegalStateException(String.format("Mindre enn %s MB ledig diskplass for %s",
                LIMIT / 1_000_000,
                absoluteFile
        ));

        /*if (absoluteFile.getFreeSpace() < LIMIT) {
            throw new IllegalStateException(String.format("Mindre enn %s MB ledig diskplass for %s",
                    LIMIT / 1_000_000,
                    absoluteFile
            ));
        }*/
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                String.format("Diskplass for sti: %s", absoluteFile.getAbsolutePath()),
                String.format("Sjekk for om det er mindre enn %s displass ledig", LIMIT / 1_000_000),
                false
        );
    }

}
