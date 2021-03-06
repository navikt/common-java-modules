package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.json.JsonUtils;
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
    public void should_serialize_enhetId_to_json_field_with_json_utils() {
        EnhetIdWrapper wrapper = new EnhetIdWrapper(EnhetId.of("1234"));
        assertEquals("{\"enhetId\":\"1234\"}", JsonUtils.toJson(wrapper));
    }

    @Test
    public void should_deserialize_json_to_enhetId_field() throws JsonProcessingException {
        String wrapperJson = "{\"enhetId\":\"1234\"}";

        EnhetIdWrapper wrapper = mapper.readValue(wrapperJson, EnhetIdWrapper.class);

        assertEquals(wrapper.getEnhetId().get(), "1234");
    }

    @Test
    public void should_deserialize_json_to_enhetId_field_with_json_utils() {
        String wrapperJson = "{\"enhetId\":\"1234\"}";

        EnhetIdWrapper wrapper = JsonUtils.fromJson(wrapperJson, EnhetIdWrapper.class);

        assertEquals(wrapper.getEnhetId().get(), "1234");
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
        String nullJson = "null";

        EnhetId enhetId = mapper.readValue(nullJson, EnhetId.class);

        assertNull(enhetId);
    }

    private static class EnhetIdWrapper {
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
