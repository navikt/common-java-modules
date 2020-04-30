package no.nav.common.cxf.saml;

import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.Subject;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.Optional;
import java.util.UUID;

import static no.nav.common.auth.subject.SsoToken.Type.SAML;
import static org.assertj.core.api.Assertions.assertThat;


public class SamlUtilsTest {

    @Test
    public void smoketest() {
        IdentType identType = IdentType.values()[0];
        String consumerId = "consumerId-" + UUID.randomUUID().toString();
        String issuer = "issuer-" + UUID.randomUUID().toString();
        String username = "username-" + UUID.randomUUID().toString();
        int authenticationLevel = 4;

        Assertion assertion = AssertionBuilder.getSamlAssertionForUsername(AssertionBuilder.Parameters.builder()
                .authenticationLevel(authenticationLevel)
                .identType(identType)
                .consumerId(consumerId)
                .issuer(issuer)
                .username(username)
                .build()
        );

        Subject subject = SamlUtils.samlAssertionToSubject(assertion);

        assertThat(subject.getIdentType()).isEqualTo(identType);
        assertThat(subject.getUid()).isEqualTo(username);

        Optional<String> optionalSamlToken = subject.getSsoToken(SAML);

        assertThat(optionalSamlToken).isNotEmpty();
        String samlToken = optionalSamlToken.get();
        assertThat(samlToken)
                .contains(identType.name())
                .contains(consumerId)
                .contains(issuer)
                .contains(username);

        assertThat(samlToken).isEqualTo(SamlUtils.getSamlAssertionAsString(SamlUtils.toSamlAssertion(samlToken)));
    }

}