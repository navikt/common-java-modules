package no.nav.dialogarena.config.fasit;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.dialogarena.config.fasit.dto.ApplicationInstance;
import no.nav.dialogarena.config.fasit.dto.DataSourceResource;
import no.nav.dialogarena.config.fasit.dto.Resource;
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
import java.io.StringReader;
import java.net.URI;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

public class FasitUtils {

    public static final String FASIT_USERNAME_VARIABLE_NAME = "domenebrukernavn";
    public static final String FASIT_PASSWORD_VARIABLE_NAME = "domenepassord";
    public static final String DEFAULT_ENVIRONMENT_VARIABLE_NAME = "testmiljo";

    private static final File fasitPropertyFile = new File(System.getProperty("user.home"), "fasit.properties");

    private static final Logger LOG = getLogger(FasitUtils.class);

    public static final String WELL_KNOWN_APPLICATION_NAME = "fasit";
    public static final String TEST_LOCAL = "test.local";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String getVariable(String variableName) {
        return ofNullable(System.getProperty(variableName, System.getenv(variableName)))
                .orElseGet(() -> getVariableFromPropertyFile(variableName));
    }

    private static String getVariableFromPropertyFile(String variableName) {
        return of(fasitPropertyFile)
                .filter(File::exists)
                .map(Util::loadProperties)
                .map(fasitProperties -> fasitProperties.getProperty(variableName))
                .orElseThrow(() -> new RuntimeException(format(
                        "mangler variabel '%s'. Denne må settes som property, miljøvariabel eller i '%s'",
                        variableName,
                        fasitPropertyFile.getAbsolutePath())
                ));
    }

    public static DbCredentials getDbCredentials(TestEnvironment testEnvironment, String applicationName) {
        List<ApplicationInstance> applications = fetchJsonObjects(String.format("https://fasit.adeo.no/api/v2/applicationinstances/application/%s", applicationName), ApplicationInstance.class);
        ApplicationInstance application = applications
                .stream()
                .filter(applicationInstance -> testEnvironment.matcher(applicationInstance.getEnvironment()))
                .findAny()
                .orElseThrow(IllegalStateException::new);
        Resource dataSourceRef = application
                .getUsedresources()
                .stream()
                .filter(resource -> "datasource".equals(resource.getType()))
                .findAny()
                .orElseThrow(IllegalStateException::new);
        DataSourceResource dataSourceResource = fetchJsonObject(dataSourceRef.ref, DataSourceResource.class);
        return new DbCredentials()
                .setUrl(dataSourceResource.properties.url)
                .setUsername(dataSourceResource.properties.username)
                .setPassword(of(fetchJson(dataSourceResource.secrets.password.ref))
                        .orElseThrow(() -> new RuntimeException("Kunne ikke finne passord for databasebruker")));
    }

    public static ServiceUserCertificate getServiceUserCertificate(String serviceUser, String environmentClass) {
        Document document = fetchXml(String.format("https://fasit.adeo.no/conf/resources?envClass=%s&type=Certificate&alias=%s&bestmatch=true", environmentClass, serviceUser));

        String alias = extractStringProperty(document, "keystorealias");
        String password = fetchJson(extractStringProperty(document, "keystorepassword"));
        byte[] keystore = fetchBytes(extractStringProperty(document, "keystore"));

        return new ServiceUserCertificate()
                .setKeystorealias(alias)
                .setKeystorepassword(password)
                .setKeystore(keystore);
    }

    public static ApplicationConfig getApplicationConfig(String applicationName, String environment) {
        Document document = fetchXml(format("https://fasit.adeo.no/conf/environments/%s/applications/%s", environment, applicationName));
        NodeList domainNodes = document.getElementsByTagName("domain");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.domain = domainNodes.item(0).getTextContent();
        LOG.info("{} = {}", applicationName, applicationConfig);
        return applicationConfig;
    }

    public static LdapConfig getLdapConfig(String ldapAlias, String applicationName, String environment) {
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=LDAP&alias=%s&app=%s",
                environment,
                applicationConfig.domain,
                ldapAlias,
                applicationName
        );

        UsernameAndPassword usernameAndPassword = getUsernameAndPassword(resourceUrl);
        LdapConfig ldapConfig = new LdapConfig()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setEnvironment(environment);

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

    public static ServiceUser getServiceUser(String userAlias, String applicationName) {
        return getServiceUser(userAlias, applicationName, getDefaultEnvironment());
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, TestEnvironment environment) {
        return getServiceUser(userAlias, applicationName, environment.toString());
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, String environment) {
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);
        return getServiceUser(userAlias, applicationName, environment, applicationConfig.domain);
    }

    public static OpenAmConfig getOpenAmConfig(String environment) {
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=OpenAm&alias=openam&app=fasit",
                environment,
                getOeraLocal(environment)
        );
        Document document = fetchXml(resourceUrl);
        UsernameAndPassword usernameAndPassword = getUsernameAndPassword(document);
        OpenAmConfig openAmConfig = new OpenAmConfig()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setRestUrl(extractStringProperty(document, "restUrl"))
                .setLogoutUrl(extractStringProperty(document, "logoutUrl"));

        LOG.info("openAm: {}", openAmConfig);
        return openAmConfig;
    }

    private static ServiceUser getServiceUser(String userAlias, String applicationName, String environment, String domain) {
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=Credential&alias=%s&app=%s",
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
                .setDomain(domain);

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

    private static String fetchJson(String url) {
        LOG.info("Fetching json: {}", url);
        String json = httpClient(httpClient -> httpClient.newRequest(url).send().getContentAsString());
        LOG.info(json.replaceAll("\n", ""));
        return json;
    }

    private static byte[] fetchBytes(String url) {
        return httpClient(httpClient -> httpClient.newRequest(url).send().getContent());
    }

    @SneakyThrows
    private static <T> T fetchJsonObject(String url, Class<T> type) {
        return objectMapper.readValue(fetchJson(url), type);
    }

    @SneakyThrows
    private static <T> List<T> fetchJsonObjects(String url, Class<T> type) {
        return objectMapper.readValue(fetchJson(url), TypeFactory.defaultInstance().constructCollectionType(List.class, type));
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
            } catch (Throwable throwable) {
                if (throwable instanceof IllegalStateException) {
                    throw throwable;
                }
                LOG.warn("feil mot fasit");
                LOG.warn(throwable.getMessage(), throwable);
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
        throw new IllegalStateException(format("fant ikke property '%s' i respons", propertyName));
    }

    public static String getFasitPassword() {
        return getVariable(FASIT_PASSWORD_VARIABLE_NAME);
    }

    public static String getFasitUser() {
        return getVariable(FASIT_USERNAME_VARIABLE_NAME);
    }

    public static String getDefaultEnvironment() {
        return getVariable(DEFAULT_ENVIRONMENT_VARIABLE_NAME);
    }

    public static String getEnvironmentClass(String environment) {
        return environment.substring(0, 1);
    }

    public static String getOeraLocal(String environment) {
        return "oera-" + getEnvironmentClass(environment) + ".local";
    }

    public static String getFSSLocal(String environment) {
        return getFSSClass(environment) + ".local";
    }

    private static String getFSSClass(String environment) {
        switch (getEnvironmentClass(environment)) {
            case "t":
                return "test";
            case "q":
                return "preprod";
            default:
                throw new IllegalStateException();
        }
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
    @Accessors(chain = true)
    public static class UsernameAndPassword {
        public String username;
        public String password;
    }

}
