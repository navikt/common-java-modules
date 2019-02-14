package no.nav.fasit.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Resource {
    public String type;
    public String ref;
}
