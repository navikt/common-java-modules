package no.nav.sbl.dialogarena.common.web.selftest.generators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;

public class SelftestJsonGenerator {
    public static String generate(Selftest selftest) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om.writeValueAsString(selftest);
    }
}
