package no.nav.modig.security.loginmodule.userinfo.openam;

import no.nav.modig.security.loginmodule.userinfo.AbstractUserInfoService;
import no.nav.modig.security.loginmodule.userinfo.UserInfo;
import no.nav.sbl.rest.RestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static no.nav.modig.security.loginmodule.userinfo.openam.OpenAMAttributes.PARAMETER_SECURITY_LEVEL;
import static no.nav.modig.security.loginmodule.userinfo.openam.OpenAMAttributes.PARAMETER_UID;

public class OpenAMUserInfoService extends AbstractUserInfoService {
    private static final Logger log = LoggerFactory.getLogger(OpenAMUserInfoService.class);

    private static final String OPENAM_GENERAL_ERROR = "Could not get user attributes from OpenAM. ";
    private static final String BASE_PATH = "/identity/json/attributes";

    private final URI endpointURL;
    protected Client client = RestUtils.createClient();

    public OpenAMUserInfoService(URI endpointURL) {
        this.endpointURL = endpointURL;
    }

    @Override
    public UserInfo getUserInfo(String subjectId) {
        String response = invokeRestClient(subjectId);
        return createUserInfo(response);
    }

    protected String invokeRestClient(String subjectId) {
        log.debug("Invoking OpenAM REST interface.");
        String url = endpointURL + BASE_PATH + format("?subjectid=%s&attributenames=%s&attributenames=%s", subjectId, PARAMETER_UID, PARAMETER_SECURITY_LEVEL);
        Response response;
        response = client.target(url).request().get();
        int status = response.getStatus();
        String phrase = response.getStatusInfo().getReasonPhrase();
        String payload = response.readEntity(String.class);
        if (status < 399) {
            log.debug("Received response: " + payload);
            return payload;
        } else {
            String message = OPENAM_GENERAL_ERROR + "HTTP status: " + status + " " + phrase + ".";
            if (status == 401) {
                message += " Response:" + payload;
            }
            log.debug(message);
            throw new OpenAMException(message);
        }
    }

    protected UserInfo createUserInfo(String response) {
        Map<String, String> attributeMap = parseUserAttributes(response);

        if (attributeMap.containsKey(PARAMETER_UID)
                && attributeMap.containsKey(PARAMETER_SECURITY_LEVEL)) {

            return new UserInfo(attributeMap.get(PARAMETER_UID),
                    Integer.valueOf(attributeMap.get(PARAMETER_SECURITY_LEVEL)));
        } else {
            throw new OpenAMException(OPENAM_GENERAL_ERROR + "Response did not contain attributes "
                    + PARAMETER_UID + " and/or " + PARAMETER_SECURITY_LEVEL);
        }
    }

    protected Map<String, String> parseUserAttributes(String response) {
        try {
            JSONObject json = new JSONObject(response);
            Object o = json.get(OpenAMAttributes.PARAMETER_ATTRIBUTES);
            Map<String, String> attributeMap = new HashMap<String, String>();
            JSONArray array = (JSONArray) o;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = (JSONObject) array.get(i);
                String name = obj.getString(OpenAMAttributes.PARAMETER_NAME);
                JSONArray values = (JSONArray) obj.get(OpenAMAttributes.PARAMETER_VALUES);
                String value = values.getString(0);
                attributeMap.put(name, value);
            }
            return attributeMap;
        } catch (JSONException e) {
            throw new OpenAMException("Error parsing JSON response. ", e);
        }
    }

}
