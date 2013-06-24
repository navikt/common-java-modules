package no.nav.sbl.dialogarena.common.kodeverk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.nav.modig.core.exception.ApplicationException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kodeverkimlpmentasjon som leser inn kodeverk på json-format
 */
public class JsonKodeverk extends BaseKodeverk {

    private static final Logger LOGGER = getLogger(JsonKodeverk.class);

    public JsonKodeverk(InputStream json) {
        try {
            traverseSkjemaerAndInsertInMap(getSkjemaer(json));
        } catch (IOException e) {
            LOGGER.error("Klarte ikke å parse kodeverk-info", e);
            throw new ApplicationException("Klarte ikke å parse kodeverk-info", e);
        }
    }

    private ArrayNode getSkjemaer(InputStream json) throws IOException {
        return (ArrayNode) new ObjectMapper().readTree(json).get("Skjemaer");
    }

    private void traverseSkjemaerAndInsertInMap(ArrayNode kodeverkArray) {
        for (JsonNode node : kodeverkArray) {
            db.put(getFieldValue(node, "Skjemanummer"),
                    new KodeverkElement(
                            getOptionalFieldValue(node, "Gosysid"),
                            getOptionalFieldValue(node, "Vedleggsid"),
                            getOptionalFieldValue(node, "Tema"),
                            getOptionalFieldValue(node, "Beskrivelse (ID)"),
                            getFieldValue(node, "Tittel"),
                            getOptionalFieldValue(node, "Lenke"),
                            getOptionalFieldValue(node, "Lenke engelsk skjema")));
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
