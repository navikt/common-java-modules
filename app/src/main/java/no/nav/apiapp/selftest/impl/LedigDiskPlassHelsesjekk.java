package no.nav.apiapp.selftest.impl;

import no.nav.apiapp.selftest.Helsesjekk;

import java.io.File;


public class LedigDiskPlassHelsesjekk implements Helsesjekk {

    private static final long LIMIT = 300_000_000L;

    @Override
    public void helsesjekk() {
        File absoluteFile = new File(".").getAbsoluteFile();
        if (absoluteFile.getFreeSpace() < LIMIT) {
            throw new IllegalStateException(String.format("Mindre enn %s MB ledig diskplass for %s",
                    LIMIT / 1_000_000,
                    absoluteFile
            ));
        }
    }

}
