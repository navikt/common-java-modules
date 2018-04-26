package no.nav.modig.security.loginmodule;

/**
* Contains info from SAML token
*/
class SamlInfo {
    private final String uid;
    private final String identType;
    private final int authLevel;
    private final String consumerId;

    public SamlInfo(String uid, String identType, int authLevel, String consumerId) {
        this.uid = uid;
        this.identType = identType;
        this.authLevel = authLevel;
        this.consumerId = consumerId;
    }

    public String getUid() {
        return uid;
    }

    public String getIdentType() {
        return identType;
    }

    public int getAuthLevel() {
        return authLevel;
    }

    public String getConsumerId() {
        return consumerId;
    }
}
