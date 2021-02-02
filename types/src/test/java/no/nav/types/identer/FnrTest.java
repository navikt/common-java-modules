package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.Fnr;
import org.junit.Test;

import static org.junit.Assert.*;

public class FnrTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_fnr_to_json_field() {
        FnrWrapper wrapper = new FnrWrapper(Fnr.of("123534252"));
        assertEquals("{\"fnr\":\"123534252\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_serialize_fnr_to_json_field_with_json_utils() {
        FnrWrapper wrapper = new FnrWrapper(Fnr.of("123534252"));
        assertEquals("{\"fnr\":\"123534252\"}", JsonUtils.toJson(wrapper));
    }

    @Test
    public void should_deserialize_json_to_fnr_field() throws JsonProcessingException {
        String FnrWrapperJson = "{\"fnr\":\"123534252\"}";

        FnrWrapper wrapper = mapper.readValue(FnrWrapperJson, FnrWrapper.class);

        assertEquals(wrapper.getFnr().get(), "123534252");
    }

    @Test
    public void should_deserialize_json_to_fnr_field_with_json_utils() {
        String FnrWrapperJson = "{\"fnr\":\"123534252\"}";

        FnrWrapper wrapper = JsonUtils.fromJson(FnrWrapperJson, FnrWrapper.class);

        assertEquals(wrapper.getFnr().get(), "123534252");
    }

    @Test
    public void should_deserialize_no_fnr() throws JsonProcessingException {
        String FnrWrapperJson = "{}";

        FnrWrapper wrapper = mapper.readValue(FnrWrapperJson, FnrWrapper.class);

        assertNull(wrapper.getFnr());
    }

    @Test
    public void should_deserialize_fnr_wrapper_null() throws JsonProcessingException {
        String FnrWrapperJson = "{\"fnr\":null}";

        FnrWrapper wrapper = mapper.readValue(FnrWrapperJson, FnrWrapper.class);

        assertNull(wrapper.getFnr());
    }

    @Test
    public void should_deserialize_fnr_field_null() throws JsonProcessingException {
        String nullJson = "null";

        Fnr fnr = mapper.readValue(nullJson, Fnr.class);

        assertNull(fnr);
    }

    @Test
    public void should_not_throw_on_valid_fnr() {
        Fnr fnr = Fnr.ofValidFnr("12345678901");
        assertNotNull(fnr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_fnr() {
        Fnr.ofValidFnr("aaaaaaaaaa");
    }

    private static class FnrWrapper {
        public Fnr fnr;

        public FnrWrapper() {
            fnr = null;
        }

        public FnrWrapper(Fnr fnr) {
            this.fnr = fnr;
        }

        public Fnr getFnr() {
            return fnr;
        }
    }

}
