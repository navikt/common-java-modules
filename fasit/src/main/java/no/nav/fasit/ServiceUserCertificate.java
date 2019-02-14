package no.nav.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ServiceUserCertificate {
    public String keystorealias;
    public String keystorepassword;
    public byte[] keystore;
}
