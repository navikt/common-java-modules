package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.types.identer.Fnr;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FnrTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_fnr_to_json_field() {
        FnrWrapper wrapper = new FnrWrapper(Fnr.of("123534252"));
        assertEquals("{\"fnr\":\"123534252\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_deserialize_json_to_fnr_field() throws JsonProcessingException {
        String fnrWrapperJson = "{\"fnr\":\"123534252\"}";

        FnrWrapper wrapper = mapper.readValue(fnrWrapperJson, FnrWrapper.class);

        assertEquals(wrapper.fnr.get(), "123534252");
    }

    @Test
    public void should_deserialize_no_fnr() throws JsonProcessingException {
        String fnrWrapperJson = "{}";

        FnrWrapper wrapper = mapper.readValue(fnrWrapperJson, FnrWrapper.class);

        assertNull(wrapper.getFnr());
    }

    @Test
    public void should_deserialize_fnr_wrapper_null() throws JsonProcessingException {
        String fnrWrapperJson = "{\"fnr\":null}";

        FnrWrapper wrapper = mapper.readValue(fnrWrapperJson, FnrWrapper.class);

        assertNull(wrapper.getFnr());
    }

    @Test
    public void should_deserialize_fnr_field_null() throws JsonProcessingException {
        String fnrJson = "null";

        Fnr fnr = mapper.readValue(fnrJson, Fnr.class);

        assertNull(fnr);
    }

    public static class FnrWrapper {
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