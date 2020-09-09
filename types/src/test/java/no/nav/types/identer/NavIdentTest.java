package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.types.identer.NavIdent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NavIdentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_navIdent_to_json_field() {
        NavIdentWrapper wrapper = new NavIdentWrapper(NavIdent.of("Z123456"));
        assertEquals("{\"navIdent\":\"Z123456\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_deserialize_json_to_navIdent_field() throws JsonProcessingException {
        String wrapperJson = "{\"navIdent\":\"Z123456\"}";
        NavIdentWrapper wrapper = mapper.readValue(wrapperJson, NavIdentWrapper.class);

        assertEquals(wrapper.navIdent.get(), "Z123456");
    }

    @Test
    public void should_deserialize_no_navIdent() throws JsonProcessingException {
        String wrapperJson = "{}";

        NavIdentWrapper wrapper = mapper.readValue(wrapperJson, NavIdentWrapper.class);

        assertNull(wrapper.getNavIdent());
    }

    @Test
    public void should_deserialize_navIdent_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"navIdent\":null}";

        NavIdentWrapper wrapper = mapper.readValue(wrapperJson, NavIdentWrapper.class);

        assertNull(wrapper.getNavIdent());
    }

    @Test
    public void should_deserialize_navIdent_field_null() throws JsonProcessingException {
        String navIdentJson = "null";

        NavIdent navIdent = mapper.readValue(navIdentJson, NavIdent.class);

        assertNull(navIdent);
    }

    public static class NavIdentWrapper {
        public NavIdent navIdent;

        public NavIdentWrapper() {
            navIdent = null;
        }

        public NavIdentWrapper(NavIdent navIdent) {
            this.navIdent = navIdent;
        }

        public NavIdent getNavIdent() {
            return navIdent;
        }
    }

}
