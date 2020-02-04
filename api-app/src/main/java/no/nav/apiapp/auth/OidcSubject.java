package no.nav.apiapp.auth;

import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

public class OidcSubject {

    private final IDTokenClaimsSet subjectClaims;

    public OidcSubject(IDTokenClaimsSet subjectClaims) {
        this.subjectClaims = subjectClaims;
    }

}
