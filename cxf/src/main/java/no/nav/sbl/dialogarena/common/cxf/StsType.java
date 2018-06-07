package no.nav.sbl.dialogarena.common.cxf;

public enum StsType {
    SYSTEM_USER_IN_FSS,
    ON_BEHALF_OF_WITH_JWT,
    EXTERNAL_SSO;

    public boolean allowCachingInEndpoint() {
        return this == SYSTEM_USER_IN_FSS;
    }

}
