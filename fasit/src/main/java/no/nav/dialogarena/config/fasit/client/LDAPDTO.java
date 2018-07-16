package no.nav.dialogarena.config.fasit.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.core.GenericType;
import java.util.List;

@Data
@Accessors(chain = true)
public class LDAPDTO {
    public static final GenericType<List<LDAPDTO>> LIST_TYPE = new GenericType<List<LDAPDTO>>() {};

    public String alias;
    public Scope scope;
    public Secrets secrets;
    public Properties properties;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String url;
        public String username;
        public String basedn;
    }

    @Data
    @Accessors(chain = true)
    public static class Secrets {
        public Password password;
    }

    @Data
    @Accessors(chain = true)
    public static class Password {
        public String ref;
    }

    @Data
    @Accessors(chain = true)
    public static class Scope {
        public String zone;
        public String environmentclass;
    }

}
