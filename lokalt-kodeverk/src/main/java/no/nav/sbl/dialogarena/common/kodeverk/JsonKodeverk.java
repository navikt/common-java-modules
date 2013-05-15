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
            ArrayNode kodeverkArray = (ArrayNode) jsonNode.get("kodeverk");

            for (JsonNode node : kodeverkArray) {
                db.put(getFieldValue(node, "skjemaNummer"),
                        new KodeverkElement(
                                getFieldValue(node, "gosysId"),
                                getFieldValue(node, "tema"),
                                getFieldValue(node, "beskrivelseNokkel"),
                                getFieldValue(node, "tittel"),
                                getFieldValue(node, "url")));
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
            throw new ApplicationException("Mangler felt " + fieldName + " i json");
        }
    }
}
