package no.nav.sbl.dialogarena.common.abac.pep;


import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HttpLogger {
    private static final Logger LOG = LoggerFactory.getLogger(HttpLogger.class);
    private final String linebreak = System.getProperty("line.separator");

    public void logPostRequest(HttpPost httpPost) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append(linebreak)
                .append(httpPost.getMethod())
                .append(linebreak);

        appendHeaders(httpPost.getAllHeaders(), stringBuilder);

        try {
            stringBuilder
                    .append(linebreak)
                    .append(httpPost.getURI())
                    .append(linebreak)
                    .append(EntityUtils.toString(httpPost.getEntity()));
        } catch (IOException e) {
            LOG.warn("Logging of request body failed");
        }
        LOG.info(stringBuilder.toString());
    }

    public void logHttpResponse(HttpResponse httpResponse) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append(linebreak)
                .append(httpResponse.getStatusLine().getStatusCode())
                .append(" ")
                .append(httpResponse.getStatusLine().getReasonPhrase())
                .append(linebreak);

        appendHeaders(httpResponse.getAllHeaders(), stringBuilder);

        String body = "";
        try {
            body = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            LOG.warn("Logging of response body failed");
        }
        stringBuilder.append(body);

        resetContentInputStream(httpResponse, body);

        LOG.info(stringBuilder.toString());

    }

    private void resetContentInputStream(HttpResponse httpResponse, String body) {
        final BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setContent(new ByteArrayInputStream(body.getBytes()));
        httpResponse.setEntity(basicHttpEntity);
    }

    private void appendHeaders(Header[] headers, StringBuilder stringBuilder) {
        for (Header header : headers) {
            stringBuilder.append(header.getName())
                    .append(": ")
                    .append(header.getValue())
                    .append(linebreak)
            ;
        }
    }

}
