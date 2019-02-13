package no.nav.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WebServiceEndpoint {
    public String url;
}
