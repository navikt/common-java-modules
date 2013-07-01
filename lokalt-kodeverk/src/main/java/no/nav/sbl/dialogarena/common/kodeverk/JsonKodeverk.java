package no.nav.sbl.dialogarena.common.kodeverk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.nav.modig.core.exception.ApplicationException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
            Map<Nokkel, String> skjema = new HashMap<>();
            Map<Nokkel, String> vedlegg = new HashMap<>();
            byggOppSkjema(node, skjema);
            dbSkjema.put(getFieldValue(node, "Skjemanummer"), new KodeverkElement(skjema));
            if (!"".equals(getOptionalFieldValue(node, "Vedleggsid")))
            {
                 byggOppSkjema(node, vedlegg);
                dbVedlegg.put(getFieldValue(node, "Vedleggsid"), new KodeverkElement(vedlegg));

            }
        }
        }

    private void byggOppSkjema(JsonNode node, Map<Nokkel, String> map) {
        map.put(Nokkel.SKJEMANUMMER, getOptionalFieldValue(node, "Skjemanummer"));
        map.put(Nokkel.GOSYS_ID, getOptionalFieldValue(node, "Gosysid"));
        map.put(Nokkel.VEDLEGGSID, getOptionalFieldValue(node, "Vedleggsid"));
        map.put(Nokkel.TEMA, getOptionalFieldValue(node, "Tema"));
        map.put(Nokkel.BESKRIVELSE, getOptionalFieldValue(node, "Beskrivelse (ID)"));
        map.put(Nokkel.TITTEL, getFieldValue(node, "Tittel"));
        map.put(Nokkel.URL, getOptionalFieldValue(node, "Lenke"));
        map.put(Nokkel.URLENGLISH, getOptionalFieldValue(node, "Lenke engelsk skjema"));
        map.put(Nokkel.URLNEWNORWEGIAN, getOptionalFieldValue(node, "Lenke nynorsk skjema"));
        map.put(Nokkel.URLPOLISH, getOptionalFieldValue(node, "Lenke polsk skjema"));
        map.put(Nokkel.URLFRENCH, getOptionalFieldValue(node, "Lenke fransk skjema"));
        map.put(Nokkel.URLSPANISH, getOptionalFieldValue(node, "Lenke spansk skjema"));
        map.put(Nokkel.URLGERMAN, getOptionalFieldValue(node, "Lenke tysk skjema"));
    }



    private String getFieldValue(JsonNode node, String fieldName) {

        if (node.has(fieldName)) {
            return node.get(fieldName).asText();
        } else {
            LOGGER.error("Mangler obligatorisk felt {} i kodeverket (json)");
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
