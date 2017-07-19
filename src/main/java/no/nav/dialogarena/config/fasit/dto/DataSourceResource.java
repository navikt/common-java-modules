package no.nav.dialogarena.config.fasit.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataSourceResource {
    public Properties properties;
    public Secrets secrets;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String url;
        public String username;
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
}
