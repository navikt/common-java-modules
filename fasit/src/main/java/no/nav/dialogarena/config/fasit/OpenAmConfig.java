package no.nav.dialogarena.config.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OpenAmConfig {
    public String username;
    public String password;
    public String restUrl;
    public String logoutUrl;
}
