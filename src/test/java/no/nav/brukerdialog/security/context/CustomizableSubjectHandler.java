package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.IdentType;

import javax.security.auth.Subject;

public class CustomizableSubjectHandler extends TestSubjectHandler {
    private Subject subject;
    private static String internSsoToken;
    private static String uid = "Z999999";
    private static String consumerId = "srvServicebruker";
    private static IdentType identType;
    private static Integer authenticationLevel = 4;

    public static void setUid(String _uid) {
        uid = _uid;
    }

    public static void setConsumerId(String _consumerId) {
        consumerId = _consumerId;
    }

    public static void setInternSsoToken(String _internSsoToken) {
        internSsoToken = _internSsoToken;
    }

    public static void setIdentType(IdentType _identType) {
        identType = _identType;
    }

    public static void setAuthenticationLevel(Integer _authenticationLevel) {
        authenticationLevel = _authenticationLevel;
    }

    @Override
    public Subject getSubject() {
        return new Subject();
    }

    @Override
    public void setSubject(Subject newSubject) {
        subject = newSubject;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public String getConsumerId() {
        return consumerId;
    }

    @Override
    public String getInternSsoToken() {
       return internSsoToken;
    }

    @Override
    public IdentType getIdentType() {
        return identType;
    }

    @Override
    public Integer getAuthenticationLevel() {
        return authenticationLevel;
    }

    @Override
    public void reset() {
        setSubject(subject);
    }
}