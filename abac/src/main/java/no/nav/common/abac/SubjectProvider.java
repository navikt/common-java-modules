package no.nav.common.abac;

public interface SubjectProvider {
    String getSubjectFromToken(String idToken);
}
