package no.nav.sbl.dialogarena.common.cxf;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;

import no.nav.modig.security.ws.SAMLInInterceptor;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.security.wss4j.KerberosTokenInterceptor;

public class CXFEndpoint {

	public final JaxWsServerFactoryBean factoryBean;
	public static final String tokenProperty = "no.nav.sbl.dialogarena.common.cxf.cxfendpoint.logging.logg-tokeninheader";

	public CXFEndpoint() {
		boolean loggTokenIHeader = Boolean.getBoolean(tokenProperty);
		boolean maskerTokenIHeader = !loggTokenIHeader;
		factoryBean = new JaxWsServerFactoryBean();
		Map<String, Object> properties = new HashMap<>();
		properties.put("schema-validation-enabled", true);
		factoryBean.setProperties(properties);
		factoryBean.getInInterceptors().add(new SAMLInInterceptor());
		LoggingFeatureUtenTokenLogging loggingFeatureUtenTokenLogging = new LoggingFeatureUtenTokenLogging();
		loggingFeatureUtenTokenLogging.setMaskerTokenIHeader(maskerTokenIHeader);
		factoryBean.setFeatures(asList(loggingFeatureUtenTokenLogging, new WSAddressingFeature()));
	}

	public CXFEndpoint enableMtom() {
		factoryBean.getProperties().put("mtom-enabled", true);
		factoryBean.getOutInterceptors().add(new AttachmentCleanupInterceptor());
		factoryBean.getOutFaultInterceptors().add(new AttachmentCleanupInterceptor());
		return this;
	}

    public CXFEndpoint kerberosInInterceptor() {
        factoryBean.getInInterceptors().clear();
        factoryBean.getInInterceptors().add(new KerberosTokenInterceptor());
        return this;
    }

	public CXFEndpoint address(String address) {
		factoryBean.setAddress(address);
		return this;
	}

	public CXFEndpoint serviceBean(Object serviceBean) {
		factoryBean.setServiceBean(serviceBean);
		return this;
	}

	public CXFEndpoint setProperty(String key, Object value) {
		factoryBean.getProperties().put(key, value);
		return this;
	}

	public void create() {
		factoryBean.create();
	}

}
