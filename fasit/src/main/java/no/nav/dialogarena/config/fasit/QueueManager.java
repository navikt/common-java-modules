package no.nav.dialogarena.config.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class QueueManager implements Scoped {
    private String name;
    private String hostname;
    private int port;

    public String environment;
}
