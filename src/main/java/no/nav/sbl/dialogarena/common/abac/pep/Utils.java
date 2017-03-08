package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.exception.InvalidJsonContent;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;

import java.io.*;

import static java.lang.System.getProperty;


public class Utils {

    public static String entityToString(HttpEntity stringEntity) {
        final InputStream content;
        String result;
        try {
            content = stringEntity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(content));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            result = sb.toString();
        } catch (IOException e) {
            throw new InvalidJsonContent(e.getMessage());
        }
        return result;

    }

    static boolean invalidClientValues(Client client) {
        return client.getDomain() == null || client.getFnr() == null || client.getCredentialResource() == null ||
                (client.getOidcToken() == null && client.getSubjectId() == null);
    }

    public static String getApplicationProperty(String propertyKey) {
        final String property = getProperty(propertyKey);
        if (StringUtils.isEmpty(property)) {
            throw new RuntimeException("Cannot find URL to abac. Verify that property " + propertyKey + " is set.");
        }
        return property;
    }
}
