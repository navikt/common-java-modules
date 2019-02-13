package no.nav.fasit;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LdapConfig {
    public String url;
    public String username;
    public String password;
    public String baseDN;
}
