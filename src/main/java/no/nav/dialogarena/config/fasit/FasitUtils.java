package no.nav.dialogarena.config.fasit;


import lombok.Data;
import lombok.SneakyThrows;
import no.nav.dialogarena.config.util.Util;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

public class FasitUtils {

    public static final String FASIT_USERNAME_VARIABLE_NAME = "domenebrukernavn";
    public static final String FASIT_PASSWORD_VARIABLE_NAME = "domenepassord";

    private static final File fasitPropertyFile = new File(System.getProperty("user.home"), "fasit.properties");

    private static final Logger LOG = getLogger(FasitUtils.class);

    public static final String WELL_KNOWN_APPLICATION_NAME = "fasit";
    public static final String TEST_LOCAL = "test.local";

    public static String getVariable(String variableName) {
        return ofNullable(System.getProperty(variableName, System.getenv(variableName)))
                .orElseGet(() -> getVariableFromPropertyFile(variableName));
    }

    private static String getVariableFromPropertyFile(String variableName) {
        return of(fasitPropertyFile)
                .filter(File::exists)
                .map(Util::loadProperties)
                .map(fasitProperties -> fasitProperties.getProperty(variableName))
                .orElseThrow(() -> new RuntimeException(String.format(
                        "mangler variabel '%s'. Denne må settes som property, miljøvariabel eller i '%s'",
                        variableName,
                        fasitPropertyFile.getAbsolutePath())
                ));
    }

    public static ApplicationConfig getApplicationConfig(String applicationName, String environment) {
        Document document = fetchXml(String.format("https://fasit.adeo.no/conf/environments/%s/applications/%s", environment, applicationName));
        NodeList domainNodes = document.getElementsByTagName("domain");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.domain = domainNodes.item(0).getTextContent();
        LOG.info("{} = {}", applicationName, applicationConfig);
        return applicationConfig;
    }

    public static LdapConfig getLdapConfig(String ldapAlias, String applicationName, String environment) {
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);
        String resourceUrl = String.format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=LDAP&alias=%s&app=%s",
                environment,
                applicationConfig.domain,
                ldapAlias,
                applicationName
        );

        UsernameAndPassword usernameAndPassword = getUsernameAndPassword(resourceUrl);
        LdapConfig ldapConfig = new LdapConfig()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())

                .setEnvironment(environment)
                ;

        LOG.info("{} = {}", ldapAlias, ldapConfig);
        return ldapConfig;
    }

    public static TestUser getTestUser(String userAlias) {
        ServiceUser serviceUser = getServiceUser(
                userAlias,
                WELL_KNOWN_APPLICATION_NAME,
                "t1",
                TEST_LOCAL
        );
        return new TestUser()
                .setUsername(serviceUser.username)
                .setPassword(serviceUser.password);
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, String environment) {
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);
        return getServiceUser(userAlias, applicationName, environment, applicationConfig.domain);
    }

    public static OpenAmConfig getOpenAmConfig(String environment) {
        String resourceUrl = String.format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=OpenAm&alias=openam&app=fasit",
                environment,
                getOeraLocal(environment)
        );
        Document document = fetchXml(resourceUrl);
        UsernameAndPassword usernameAndPassword = getUsernameAndPassword(document);
        OpenAmConfig openAmConfig = new OpenAmConfig()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setRestUrl(extractStringProperty(document,"restUrl"))
                .setLogoutUrl(extractStringProperty(document,"logoutUrl"))
                ;

        LOG.info("openAm: {}",  openAmConfig);
        return openAmConfig;
    }

    private static ServiceUser getServiceUser(String userAlias, String applicationName, String environment, String domain) {
        String resourceUrl = String.format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=Credential&alias=%s&app=%s",
                environment,
                domain,
                userAlias,
                applicationName
        );
        UsernameAndPassword usernameAndPassword = getUsernameAndPassword(resourceUrl);
        ServiceUser serviceUser = new ServiceUser()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setEnvironment(environment)
                .setDomain(domain)
                ;

        LOG.info("{} = {}", userAlias, serviceUser);
        return serviceUser;
    }

    private static UsernameAndPassword getUsernameAndPassword(String resourceUrl) {
        return getUsernameAndPassword(fetchXml(resourceUrl));
    }

    private static UsernameAndPassword getUsernameAndPassword(Document document) {
        UsernameAndPassword usernameAndPassword = new UsernameAndPassword();
        usernameAndPassword.setUsername(extractStringProperty(document, "username"));

        String passwordUrl = extractStringProperty(document, "password");
        LOG.info("fetching password from: {}", passwordUrl);
        usernameAndPassword.setPassword(httpClient(httpClient -> getContent(httpClient
                .newRequest(passwordUrl)
                .send()))
        );
        return usernameAndPassword;
    }

    private static String getContent(ContentResponse contentResponse) {
        String contentAsString = contentResponse.getContentAsString();
        if (contentResponse.getStatus() != 200) {
            throw new IllegalStateException(contentAsString);
        } else {
            return contentAsString;
        }
    }

    private static Document fetchXml(String resourceUrl) {
        LOG.info("Fetching xml: {}", resourceUrl);
        return httpClient(httpClient -> {
            ContentResponse contentResponse = httpClient
                    .newRequest(resourceUrl)
                    .send();

            String resourceXml = getContent(contentResponse);

            LOG.info(resourceXml);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new InputSource(new StringReader(resourceXml)));
        });
    }

    @SneakyThrows
    public static <T> T httpClient(Util.With<HttpClient, T> httpClientConsumer) {
        int attempt = 0;
        do {
            try {
                return invokeHttpClient(httpClientConsumer);
            } catch (IOException ioException) {
                LOG.warn("ssl exception. Retry");
                LOG.warn(ioException.getMessage(), ioException);
                Thread.sleep(5000L);
            }
        } while (attempt++ < 5);
        throw new IllegalStateException("Klarer ikke å snakke med Fasit");
    }

    private static <T> T invokeHttpClient(Util.With<HttpClient, T> httpClientConsumer) throws SSLException {
        return Util.httpClient((httpClient) -> {
            httpClient.getAuthenticationStore().addAuthentication(new FasitAuthenication());
            return httpClientConsumer.with(httpClient);
        });
    }

    private static String extractStringProperty(Document document, String propertyName) {
        NodeList properties = document.getElementsByTagName("property");
        return extractStringProperty(properties, propertyName);
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

    public static String getEnvironmentClass(String environment) {
        return environment.substring(0, 1);
    }

    public static String getOeraLocal(String environment) {
        return "oera-" + getEnvironmentClass(environment) + ".local";
    }

    public static boolean erEksterntDomene(String domain) {
        return domain != null && domain.contains("oera");
    }

    private static class FasitAuthenication extends BasicAuthentication {

        private FasitAuthenication() {
            super(URI.create("https://fasit.adeo.no"), null, getFasitUser(), getFasitPassword());
        }

        @Override
        public boolean matches(String type, URI uri, String realm) {
            return true;
        }

    }

    @Data
    private static class UsernameAndPassword {
        public String username;
        public String password;
    }

}
