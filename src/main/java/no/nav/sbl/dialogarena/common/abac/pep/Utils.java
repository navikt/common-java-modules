package no.nav.sbl.dialogarena.common.abac.pep;

import org.apache.http.HttpEntity;

import java.io.*;


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
}
