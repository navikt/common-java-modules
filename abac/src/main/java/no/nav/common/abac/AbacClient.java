package no.nav.common.abac;

public interface AbacClient {

    String sendRequest(String xacmlRequestJson);

}
