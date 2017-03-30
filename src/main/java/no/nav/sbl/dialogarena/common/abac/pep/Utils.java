package no.nav.sbl.dialogarena.common.abac.pep;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;

import java.io.*;

import static java.lang.System.getProperty;


public class Utils {

    public static String entityToString(HttpEntity stringEntity) throws IOException {
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
            throw new IOException("Failed to parse json content to string", e);
        }
        return result;

    }

    static boolean invalidClientValues(Client client) {
        return client.getDomain() == null || client.getCredentialResource() == null ||
                (client.getOidcToken() == null && client.getSubjectId() == null);
    }

    public static String getApplicationProperty(String propertyKey) throws NoSuchFieldException {
        final String property = getProperty(propertyKey);
        if (StringUtils.isEmpty(property)) {
            throw new NoSuchFieldException("Cannot find property. Verify that property \"" + propertyKey + "\" is set.");
        }
        return property;
    }
}
