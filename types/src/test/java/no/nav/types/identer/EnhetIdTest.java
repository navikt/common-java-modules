package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.types.identer.EnhetId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EnhetIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_enhetId_to_json_field() {
        EnhetIdWrapper wrapper = new EnhetIdWrapper(EnhetId.of("1234"));
        assertEquals("{\"enhetId\":\"1234\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_deserialize_json_to_enhetId_field() throws JsonProcessingException {
        String wrapperJson = "{\"enhetId\":\"1234\"}";

        EnhetIdWrapper wrapper = mapper.readValue(wrapperJson, EnhetIdWrapper.class);

        assertEquals(wrapper.enhetId.get(), "1234");
    }

    @Test
    public void should_deserialize_no_enhetId() throws JsonProcessingException {
        String wrapperJson = "{}";

        EnhetIdWrapper wrapper = mapper.readValue(wrapperJson, EnhetIdWrapper.class);

        assertNull(wrapper.getEnhetId());
    }

    @Test
    public void should_deserialize_enhetId_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"enhetId\":null}";

        EnhetIdWrapper wrapper = mapper.readValue(wrapperJson, EnhetIdWrapper.class);

        assertNull(wrapper.getEnhetId());
    }

    @Test
    public void should_deserialize_enhetId_field_null() throws JsonProcessingException {
        String enhetIdJson = "null";

        EnhetId enhetId = mapper.readValue(enhetIdJson, EnhetId.class);

        assertNull(enhetId);
    }

    public static class EnhetIdWrapper {
        public EnhetId enhetId;

        public EnhetIdWrapper() {
            enhetId = null;
        }

        public EnhetIdWrapper(EnhetId enhetId) {
            this.enhetId = enhetId;
        }

        public EnhetId getEnhetId() {
            return enhetId;
        }
    }

}
