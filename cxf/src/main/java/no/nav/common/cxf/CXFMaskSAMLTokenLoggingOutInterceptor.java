package no.nav.common.cxf;

import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class CXFMaskSAMLTokenLoggingOutInterceptor extends LoggingOutInterceptor {

    private boolean maskerSAMLToken = true;

    public CXFMaskSAMLTokenLoggingOutInterceptor() {
        super();
        this.sender = new MaskingSender(new Slf4jVerboseEventSender());
    }

    public CXFMaskSAMLTokenLoggingOutInterceptor(int limit) {
        super();
        setLimit(limit);
        this.sender = new MaskingSender(new Slf4jVerboseEventSender());
    }

    public void setMaskerSAMLToken(boolean maskerSAMLToken) {
        this.maskerSAMLToken = maskerSAMLToken;
    }

    /**
     * Fjerner SAML-tokens (wsse:Security) fra XML-payloaden i LogEvent-et
     * før logging. Pakke-privat for direkte testing.
     */
    void maskIfEnabled(LogEvent event) {
        String payload = event.getPayload();
        String contentType = event.getContentType();
        if (maskerSAMLToken
                && payload != null
                && contentType != null
                && contentType.contains("xml")) {
            event.setPayload(removeSAMLTokenFromXML(payload));
        }
    }

    private String removeSAMLTokenFromXML(String xmlString) {
        Document document = Jsoup.parse(xmlString, "", Parser.xmlParser());
        for (Element element : document.getElementsByTag("soap:header").select("*")) {
            if (element.tagName().toLowerCase().endsWith(":security")) {
                element.remove();
            }
        }
        return document.toString();
    }

    /**
     * Wrapper som fjerner SAML-tokens (wsse:Security o.l.) fra SOAP-payloaden
     * før meldingen blir logget, slik at tokens ikke havner i loggene.
     */
    private class MaskingSender implements LogEventSender {

        private final LogEventSender delegate;

        MaskingSender(LogEventSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public void send(LogEvent event) {
            maskIfEnabled(event);
            delegate.send(event);
        }
    }
}
