package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.types.identer.AktorId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AktorIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_aktorId_to_json_field() {
        AktorIdWrapper wrapper = new AktorIdWrapper(AktorId.of("123534252"));
        assertEquals("{\"aktorId\":\"123534252\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_deserialize_json_to_aktorId_field() throws JsonProcessingException {
        String wrapperJson = "{\"aktorId\":\"123534252\"}";

        AktorIdWrapper wrapper = mapper.readValue(wrapperJson, AktorIdWrapper.class);

        assertEquals(wrapper.aktorId.get(), "123534252");
    }

    @Test
    public void should_deserialize_no_aktorId() throws JsonProcessingException {
        String wrapperJson = "{}";

        AktorIdWrapper wrapper = mapper.readValue(wrapperJson, AktorIdWrapper.class);

        assertNull(wrapper.getAktorId());
    }

    @Test
    public void should_deserialize_aktorId_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"aktorId\":null}";

        AktorIdWrapper wrapper = mapper.readValue(wrapperJson, AktorIdWrapper.class);

        assertNull(wrapper.getAktorId());
    }

    @Test
    public void should_deserialize_aktorId_field_null() throws JsonProcessingException {
        String aktorIdJson = "null";

        AktorId aktorId = mapper.readValue(aktorIdJson, AktorId.class);

        assertNull(aktorId);
    }

    public static class AktorIdWrapper {
        public AktorId aktorId;

        public AktorIdWrapper() {
            aktorId = null;
        }

        public AktorIdWrapper(AktorId aktorId) {
            this.aktorId = aktorId;
        }

        public AktorId getAktorId() {
            return aktorId;
        }
    }

}
