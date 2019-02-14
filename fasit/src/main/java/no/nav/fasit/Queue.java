package no.nav.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Queue implements Scoped {
    private String name;

    public String environment;
    public String environmentClass;
}
