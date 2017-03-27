package no.nav.dialogarena.config.fasit;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

public class FasitUtils {

    public static final String FASIT_USERNAME_VARIABLE_NAME = "domenebrukernavn";
    public static final String FASIT_PASSWORD_VARIABLE_NAME = "domenepassord";

    private static final Logger LOG = getLogger(FasitUtils.class);
    private static final SslContextFactory SSL_CONTEXT_FACTORY = new SslContextFactory();

    public static String getVariable(String variableName) {
        return ofNullable(System.getProperty(variableName, System.getenv(variableName)))
                .orElseThrow(() -> new RuntimeException(String.format("mangler '%s'. Denne må settes som property eller miljøvariabel", variableName)));
    }

    public static ApplicationConfig getApplicationConfig(String applicationName, String environment) {
        Document document = fetchXml(String.format("https://fasit.adeo.no/conf/environments/%s/applications/%s", environment, applicationName));
        NodeList domainNodes = document.getElementsByTagName("domain");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.domain = domainNodes.item(0).getTextContent();
        LOG.info("{} = {}", applicationName, applicationConfig);
        return applicationConfig;
    }

    public static ServiceUser getServiceUser(String username, String applicationName, String environment) {
        ServiceUser serviceUser = new ServiceUser();
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);

        Document document = fetchXml(String.format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=Credential&alias=%s&app=%s",
                environment,
                applicationConfig.domain,
                username,
                applicationName
        ));

        NodeList properties = document.getElementsByTagName("property");
        serviceUser.username = extractStringProperty(properties, "username");

        String passwordUrl = extractStringProperty(properties, "password");
        LOG.info(passwordUrl);

        httpClient(httpClient -> serviceUser.password = httpClient
                .newRequest(passwordUrl)
                .send()
                .getContentAsString());

        LOG.info("{} = {}", username, serviceUser);
        return serviceUser;
    }

    private static Document fetchXml(String resourceUrl) {
        LOG.info("Fetching xml: {}", resourceUrl);
        return httpClient(httpClient -> {
            String resourceXml = httpClient
                    .newRequest(resourceUrl)
                    .send()
                    .getContentAsString();

            LOG.info(resourceXml);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new InputSource(new StringReader(resourceXml)));
        });
    }

    private static <T> T handle(Exception exception) {
        throw new RuntimeException(exception);
    }

    private static <T> T httpClient(With<HttpClient, T> httpClientConsumer) {
        HttpClient httpClient = new HttpClient(SSL_CONTEXT_FACTORY);
        httpClient.getAuthenticationStore().addAuthentication(new FasitAuthenication());
        try {
            httpClient.start();
            return httpClientConsumer.with(httpClient);
        } catch (Exception e) {
            return handle(e);
        } finally {
            try {
                httpClient.stop();
            } catch (Exception e) {
                return handle(e);
            }
        }
    }


    private static String extractStringProperty(NodeList nodeList, String propertyName) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            String aPropertyName = item.getAttributes().getNamedItem("name").getTextContent();
            if (aPropertyName.equals(propertyName)) {
                return item.getFirstChild().getTextContent();
            }
        }
        throw new IllegalStateException();
    }

    static String getFasitPassword() {
        return getVariable(FASIT_PASSWORD_VARIABLE_NAME);
    }

    static String getFasitUser() {
        return getVariable(FASIT_USERNAME_VARIABLE_NAME);
    }

    private static class FasitAuthenication extends BasicAuthentication {

        public FasitAuthenication() {
            super(null, null, getFasitUser(), getFasitPassword());
        }

        @Override
        public boolean matches(String type, URI uri, String realm) {
            return true;
        }

    }

    @FunctionalInterface
    public interface With<T, R> {

        R with(T t) throws Exception;

    }

}
