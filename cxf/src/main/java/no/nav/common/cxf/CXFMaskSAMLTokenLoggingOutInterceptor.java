package no.nav.common.cxf;

import lombok.Setter;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jEventSender;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;


public class CXFMaskSAMLTokenLoggingOutInterceptor implements LogEventSender {
    @Setter
    private boolean maskerSAMLToken = true;

    private final LogEventSender delegate = new Slf4jEventSender();

    @Override
    public void send(LogEvent event) {
        if (maskerSAMLToken && event.getContentType() != null && event.getContentType().contains("xml")) {
            event.setPayload(removeSAMLTokenFromXML(event.getPayload()));
        }
        delegate.send(event);
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
}
