package no.nav.types.identer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AzureObjectId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AzureObjectIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_serialize_azureObjectId_to_json_field() {
        AzureObjectIdWrapper wrapper = new AzureObjectIdWrapper(AzureObjectId.of("123534252"));
        assertEquals("{\"azureObjectId\":\"123534252\"}", mapper.valueToTree(wrapper).toString());
    }

    @Test
    public void should_serialize_azureObjectId_to_json_field_with_json_utils() {
        AzureObjectIdWrapper wrapper = new AzureObjectIdWrapper(AzureObjectId.of("123534252"));
        assertEquals("{\"azureObjectId\":\"123534252\"}", JsonUtils.toJson(wrapper));
    }

    @Test
    public void should_deserialize_json_to_azureObjectId_field() throws JsonProcessingException {
        String wrapperJson = "{\"azureObjectId\":\"123534252\"}";

        AzureObjectIdWrapper wrapper = mapper.readValue(wrapperJson, AzureObjectIdWrapper.class);

        assertEquals(wrapper.getAzureObjectId().get(), "123534252");
    }

    @Test
    public void should_deserialize_json_to_azureObjectId_field_with_json_utils() {
        String wrapperJson = "{\"azureObjectId\":\"123534252\"}";

        AzureObjectIdWrapper wrapper = JsonUtils.fromJson(wrapperJson, AzureObjectIdWrapper.class);

        assertEquals(wrapper.getAzureObjectId().get(), "123534252");
    }

    @Test
    public void should_deserialize_no_azureObjectId() throws JsonProcessingException {
        String wrapperJson = "{}";

        AzureObjectIdWrapper wrapper = mapper.readValue(wrapperJson, AzureObjectIdWrapper.class);

        assertNull(wrapper.getAzureObjectId());
    }

    @Test
    public void should_deserialize_azureObjectId_wrapper_null() throws JsonProcessingException {
        String wrapperJson = "{\"azureObjectId\":null}";

        AzureObjectIdWrapper wrapper = mapper.readValue(wrapperJson, AzureObjectIdWrapper.class);

        assertNull(wrapper.getAzureObjectId());
    }

    @Test
    public void should_deserialize_azureObjectId_field_null() throws JsonProcessingException {
        String nullJson = "null";

        AzureObjectId azureObjectId = mapper.readValue(nullJson, AzureObjectId.class);

        assertNull(azureObjectId);
    }

    private static class AzureObjectIdWrapper {
        public AzureObjectId azureObjectId;

        public AzureObjectIdWrapper() {
            azureObjectId = null;
        }

        public AzureObjectIdWrapper(AzureObjectId azureObjectId) {
            this.azureObjectId = azureObjectId;
        }

        public AzureObjectId getAzureObjectId() {
            return azureObjectId;
        }
    }

}
