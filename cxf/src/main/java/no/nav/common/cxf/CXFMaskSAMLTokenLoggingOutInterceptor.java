package no.nav.common.cxf;

import lombok.Setter;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

@Setter
public class CXFMaskSAMLTokenLoggingOutInterceptor extends AbstractMaskingLogEventSender {
    private boolean maskerSAMLToken = true;

    @Override
    protected void mask(LogEvent event) {
        if (maskerSAMLToken && event.getContentType() != null && event.getContentType().contains("xml")) {
            event.setPayload(removeSAMLTokenFromXML(event.getPayload()));
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
}
