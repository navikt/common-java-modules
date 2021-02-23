package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.NorskIdent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NorskIdentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_norsk_ident_to_json_field() {
        NorskIdentWrapper wrapper = new NorskIdentWrapper(NorskIdent.of("123534252"));
        assertEquals("{\"norskIdent\":\"123534252\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_serialize_norsk_ident_to_json_field_with_json_utils() {
        NorskIdentWrapper wrapper = new NorskIdentWrapper(NorskIdent.of("123534252"));
        assertEquals("{\"norskIdent\":\"123534252\"}", JsonUtils.toJson(wrapper));
    }

    @Test
    public void should_deserialize_json_to_norsk_ident_field() throws JsonProcessingException {
        String wrapperJson = "{\"norskIdent\":\"123534252\"}";

        NorskIdentWrapper wrapper = mapper.readValue(wrapperJson, NorskIdentWrapper.class);

        assertEquals(wrapper.getNorskIdent().get(), "123534252");
    }

    @Test
    public void should_deserialize_json_to_norsk_ident_field_with_json_utils() {
        String wrapperJson = "{\"norskIdent\":\"123534252\"}";

        NorskIdentWrapper wrapper = JsonUtils.fromJson(wrapperJson, NorskIdentWrapper.class);

        assertEquals(wrapper.getNorskIdent().get(), "123534252");
    }

    @Test
    public void should_deserialize_no_norsk_ident() throws JsonProcessingException {
        String wrapperJson = "{}";

        NorskIdentWrapper wrapper = mapper.readValue(wrapperJson, NorskIdentWrapper.class);

        assertNull(wrapper.getNorskIdent());
    }

    @Test
    public void should_deserialize_norsk_ident_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"norskIdent\":null}";

        NorskIdentWrapper wrapper = mapper.readValue(wrapperJson, NorskIdentWrapper.class);

        assertNull(wrapper.getNorskIdent());
    }

    @Test
    public void should_deserialize_norsk_ident_field_null() throws JsonProcessingException {
        String nullJson = "null";

        NorskIdent norskIdent = mapper.readValue(nullJson, NorskIdent.class);

        assertNull(norskIdent);
    }
    
    private static class NorskIdentWrapper {
        public NorskIdent norskIdent;

        public NorskIdentWrapper() {
            norskIdent = null;
        }

        public NorskIdentWrapper(NorskIdent norskIdent) {
            this.norskIdent = norskIdent;
        }

        public NorskIdent getNorskIdent() {
            return norskIdent;
        }
    }
    
}
