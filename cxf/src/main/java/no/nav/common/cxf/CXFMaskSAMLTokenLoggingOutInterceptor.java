package no.nav.common.cxf;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.util.logging.Logger;


public class CXFMaskSAMLTokenLoggingOutInterceptor extends LoggingOutInterceptor {
    private boolean maskerSAMLToken = true;

    public CXFMaskSAMLTokenLoggingOutInterceptor() {
        super();
    }

    public CXFMaskSAMLTokenLoggingOutInterceptor(int limit) {
        super(limit);
    }

    @Override
    protected void writePayload(StringBuilder builder, CachedOutputStream cos, String encoding, String contentType, boolean truncated) throws Exception {
        if(contentType.contains("xml") && maskerSAMLToken) {
            super.writePayload(builder, getMaskedOutputStream(cos), encoding, contentType, truncated);
        } else {
            super.writePayload(builder, cos, encoding, contentType, truncated);
        }
    }

    @Override
    public void handleMessage(Message message) {
        setupMessageLogger(message);
        super.handleMessage(message);
    }

    public void setMaskerSAMLToken(boolean maskerSAMLToken) {
        this.maskerSAMLToken = maskerSAMLToken;
    }

    private void setupMessageLogger(Message message) {
        EndpointInfo endpoint = message.getExchange().getEndpoint().getEndpointInfo();
        if (endpoint.getService() != null) {
            Logger logger = endpoint.getProperty("MessageLogger", Logger.class);
            if (logger == null) {
                String serviceName = endpoint.getService().getName().getLocalPart();
                InterfaceInfo iface = endpoint.getService().getInterface();
                String portName = endpoint.getName().getLocalPart();
                String portTypeName = iface.getName().getLocalPart();
                String logName = CXFMaskSAMLTokenLoggingOutInterceptor.class.getName()
                        + "." + serviceName + "."
                        + portName + "." + portTypeName;
                logger = LogUtils.getL7dLogger(this.getClass(), null, logName);
                endpoint.setProperty("MessageLogger", logger);
            }
        }
    }

    private CachedOutputStream getMaskedOutputStream(CachedOutputStream cos) throws Exception {
        String xmlString = IOUtils.toString(cos.getInputStream());
        CachedOutputStream maskertCos = new CachedOutputStream();
        maskertCos.write(removeSAMLTokenFromXML(xmlString).getBytes());
        return maskertCos;
    }

    private String removeSAMLTokenFromXML(String xmlString) {
        Document document = Jsoup.parse(xmlString, "", Parser.xmlParser());
        for(Element element : document.getElementsByTag("soap:header").select("*")) {
            if(element.tagName().toLowerCase().endsWith(":security")) {
                element.remove();
            }
        }
        return document.toString();
    }
}
