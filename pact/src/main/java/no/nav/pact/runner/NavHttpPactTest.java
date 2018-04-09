package no.nav.pact.runner;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;

@PactBroker(
        host = "${PACT_BROKER}",
        authentication = @PactBrokerAuth(username = "${PACT_USERNAME}", password = "${PACT_PASSWORD}"),
        port = "${PACT_BROKER_PORT:80}", tags = "${PACT_TAGS:latest}")
@IgnoreNoPactsToVerify
public abstract class NavHttpPactTest extends PactHttpTarget {

}
