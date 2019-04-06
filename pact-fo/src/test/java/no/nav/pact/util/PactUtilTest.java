package no.nav.pact.util;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.matchingrules.Category;
import au.com.dius.pact.model.matchingrules.MatchingRuleGroup;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static no.nav.json.TestUtils.assertEqualJson;
import static org.assertj.core.api.Assertions.assertThat;


public class PactUtilTest {

    @Test
    public void smoketest() throws IOException {
        PactDslJsonBody pactDslJsonBody = PactUtil.dtoBody(new TestDTO());

        String body = pactDslJsonBody.getBody().toString();
        assertEqualJson(body, IOUtils.toString(PactUtilTest.class.getResourceAsStream("/pact-test-dto-body.json"), Charset.defaultCharset()));

        Category matchers = pactDslJsonBody.getMatchers();
        Map<String, MatchingRuleGroup> matchingRules = matchers.getMatchingRules();
        assertThat(matchingRules).hasSize(3);
    }

    private static class TestDTO {
        private String aString = "example string";
        private int anInt = 42;
        private boolean aBoolean = true;
    }

}