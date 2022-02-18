package no.nav.common.abac.audit;

import lombok.SneakyThrows;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.Test;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static no.nav.common.auth.Constants.ID_PORTEN_PID_CLAIM;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NimbusSubjectProviderTest {

    NimbusSubjectProvider nimbusSubjectProvider = new NimbusSubjectProvider();

    @Test
    public void returns_NAVident_claim_if_present() {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim(AAD_NAV_IDENT_CLAIM, "veilederident");
        jwtClaims.setClaim(ID_PORTEN_PID_CLAIM, "fnr");
        jwtClaims.setClaim("sub", "subject");

        assertEquals("veilederident", nimbusSubjectProvider.getSubjectFromToken(tokenMedClaim(jwtClaims)));
    }

    @Test
    public void returns_pid_claim_if_present_and_NAVident_not_present() {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim(ID_PORTEN_PID_CLAIM, "fnr");
        jwtClaims.setClaim("sub", "subject");

        assertEquals("fnr", nimbusSubjectProvider.getSubjectFromToken(tokenMedClaim(jwtClaims)));
    }

    @Test
    public void returns_sub_claim_if_present_and_pid_claim_not_present_and_NAVident_not_present() {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("sub", "subject");

        assertEquals("subject", nimbusSubjectProvider.getSubjectFromToken(tokenMedClaim(jwtClaims)));
    }

    @Test
    public void returns_null_if_sub_claim_not_present_and_pid_claim_not_present_and_NAVident_not_present() {
        JwtClaims jwtClaims = new JwtClaims();

        assertNull(nimbusSubjectProvider.getSubjectFromToken(tokenMedClaim(jwtClaims)));
    }


    @SneakyThrows
    private String tokenMedClaim(JwtClaims jwtClaims) {
        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(jwtClaims.toJson());
        jws.setKey(RsaJwkGenerator.generateJwk(2048).getPrivateKey());
        jws.setAlgorithmHeaderValue(RSA_USING_SHA256);
        return jws.getCompactSerialization();

    }
}
