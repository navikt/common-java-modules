package no.nav.common.abac.audit;

public interface SubjectProvider {
    String getSubjectFromToken(String idToken);
}
