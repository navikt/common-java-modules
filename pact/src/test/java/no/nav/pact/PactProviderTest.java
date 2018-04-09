package no.nav.pact;

import au.com.dius.pact.provider.junit.Consumer;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import no.nav.pact.runner.NavHttpsPactTest;
import no.nav.pact.runner.NavPactRunner;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/*
Example usage:

@RunWith(NavPactRunner.class)
@Provider("veilarbperson")
@Consumer("veilarbpersonfs")
public class PactProviderTest extends NavHttpsPactTest {

    @State("has a single person without children")
    public void verifyProviderStateSinglePersonNoChildren() {
        System.out.println("Single person");
    }

    @State("does not have person")
    public void verifyProviderStateNoData() {
        System.out.println("No data");
    }

    @State("is alive state")
    public void verifyIsAlive() {
        System.out.println("Is Alive?");
    }

    @Override
    public String getHttpTarget() {
        return "https://app-t6.adeo.no";
    }
}
*/

@Ignore
public class PactProviderTest {

}