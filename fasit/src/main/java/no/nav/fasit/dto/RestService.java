package no.nav.fasit.dto;

import lombok.Builder;
import lombok.Value;
import no.nav.fasit.Scoped;

@Value
@Builder
public class RestService implements Scoped {

    private String url;
    private String alias;
    private String environment;
    private String environmentClass;
    private String application;

}
