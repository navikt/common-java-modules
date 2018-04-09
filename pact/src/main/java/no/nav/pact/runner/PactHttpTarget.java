package no.nav.pact.runner;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class PactHttpTarget {

    @TestTarget
    public static Target target;

    public PactHttpTarget() {
        try {
            target = new HttpTarget(new URL(getHttpTarget()));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot set HttpTarget Url.", e);
        }
    }

    abstract public String getHttpTarget();

}
