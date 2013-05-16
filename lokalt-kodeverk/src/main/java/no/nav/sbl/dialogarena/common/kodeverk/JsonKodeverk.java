package no.nav.sbl.dialogarena.common.kodeverk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.nav.modig.core.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Kodeverkimlpmentasjon som leser inn kodeverk på json-format
 */
public class JsonKodeverk extends BaseKodeverk {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonKodeverk.class);

    public JsonKodeverk(InputStream json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(json);
            ArrayNode kodeverkArray = (ArrayNode) jsonNode.get("Skjemaer");

            for (JsonNode node : kodeverkArray) {
                db.put(getFieldValue(node, "Skjemanummer"),
                        new KodeverkElement(
                                getOptionalFieldValue(node, "GosysId"),
                                getFieldValue(node, "Tema"),
                                getFieldValue(node, "Beskrivelse"),
                                getFieldValue(node, "Tittel"),
                                getOptionalFieldValue(node, "Url")));
            }
        } catch (IOException e) {
            LOGGER.error("Klarte ikke å parse kodeverk-info", e);
            throw new ApplicationException("Klarte ikke å parse kodeverk-info", e);
        }

    }

    private String getFieldValue(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            return node.get(fieldName).asText();
        } else {
            LOGGER.error("Mangler obligatorisk felt {} i kodeverket (json)", fieldName);
            throw new ApplicationException("Mangler felt " + fieldName + " i kodeverket json");
        }
    }

    private String getOptionalFieldValue(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            return node.get(fieldName).asText();
        } else {
            return "";
        }
    }
}
