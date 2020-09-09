package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.types.identer.PersonId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PersonIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_personId_to_json_field() {
        PersonIdWrapper wrapper = new PersonIdWrapper(PersonId.of("ID123"));
        assertEquals("{\"personId\":\"ID123\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_deserialize_json_to_personId_field() throws JsonProcessingException {
        String wrapperJson = "{\"personId\":\"ID123\"}";

        PersonIdWrapper wrapper = mapper.readValue(wrapperJson, PersonIdWrapper.class);

        assertEquals(wrapper.personId.get(), "ID123");
    }

    @Test
    public void should_deserialize_no_personId() throws JsonProcessingException {
        String wrapperJson = "{}";

        PersonIdWrapper wrapper = mapper.readValue(wrapperJson, PersonIdWrapper.class);

        assertNull(wrapper.getPersonId());
    }

    @Test
    public void should_deserialize_personId_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"personId\":null}";

        PersonIdWrapper wrapper = mapper.readValue(wrapperJson, PersonIdWrapper.class);

        assertNull(wrapper.getPersonId());
    }

    @Test
    public void should_deserialize_personId_field_null() throws JsonProcessingException {
        String personIdJson = "null";

        PersonId personId = mapper.readValue(personIdJson, PersonId.class);

        assertNull(personId);
    }

    public static class PersonIdWrapper {
        public PersonId personId;

        public PersonIdWrapper() {
            personId = null;
        }

        public PersonIdWrapper(PersonId personId) {
            this.personId = personId;
        }

        public PersonId getPersonId() {
            return personId;
        }
    }

}
