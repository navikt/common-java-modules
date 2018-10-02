package no.nav.dialogarena.config.fasit.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.dialogarena.config.fasit.*;
import no.nav.dialogarena.config.fasit.dto.ApplicationInstance;
import no.nav.dialogarena.config.fasit.dto.DataSourceResource;
import no.nav.dialogarena.config.fasit.dto.Resource;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.json.JsonProvider;
import no.nav.sbl.rest.RestUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class FasitClientImpl implements FasitClient {

    private static final Logger LOG = getLogger(FasitClientImpl.class);

    private static final ObjectMapper objectMapper = JsonProvider.createObjectMapper();

    @Override
    public String getBaseUrl(FasitClient.GetBaseUrlRequest getBaseUrlRequest) {
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=BaseUrl&alias=%s&app=%s",
                getBaseUrlRequest.environment,
                getBaseUrlRequest.domain,
                getBaseUrlRequest.baseUrlAlias,
                getBaseUrlRequest.application
        );
        Document document = fetchXml(resourceUrl);
        return extractStringProperty(document, "url");
    }

    @Override
    public OpenAmConfig getOpenAmConfig(String environment) {
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=OpenAm&alias=openam&app=fasit",
                environment,
                getOeraLocal(environment)
        );
        Document document = fetchXml(resourceUrl);
        FasitUtils.UsernameAndPassword usernameAndPassword = getUsernameAndPassword(document);
        OpenAmConfig openAmConfig = new OpenAmConfig()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setRestUrl(extractStringProperty(document, "restUrl"))
                .setLogoutUrl(extractStringProperty(document, "logoutUrl"));

        return openAmConfig;
    }

    @Override
    public DbCredentials getDbCredentials(GetDbCredentialsRequest getDbCredentialsRequest) {
        List<ApplicationInstance> applications = fetchJsonObjects(String.format("https://fasit.adeo.no/api/v2/applicationinstances/application/%s", getDbCredentialsRequest.applicationName), ApplicationInstance.class);
        ApplicationInstance application = applications
                .stream()
                .filter(applicationInstance -> getDbCredentialsRequest.testEnvironment.matcher(applicationInstance.getEnvironment()))
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
                .setPassword(getPassword(dataSourceResource.secrets.password.ref));
    }

    @Override
    public ServiceUser getCredentials(GetCredentialsRequest getCredentialsRequest) {
        String resourceUrl = format("https://fasit.adeo.no/conf/resources/bestmatch?envName=%s&domain=%s&type=Credential&alias=%s&app=%s",
                getCredentialsRequest.environment,
                getCredentialsRequest.domain,
                getCredentialsRequest.userAlias,
                getCredentialsRequest.applicationName
        );
        FasitUtils.UsernameAndPassword usernameAndPassword = getUsernameAndPassword(resourceUrl);
        ServiceUser serviceUser = new ServiceUser()
                .setUsername(usernameAndPassword.getUsername())
                .setPassword(usernameAndPassword.getPassword())
                .setEnvironment(getCredentialsRequest.environment)
                .setDomain(getCredentialsRequest.domain);

        LOG.info("{} = {}", getCredentialsRequest.userAlias, serviceUser);
        return serviceUser;
    }

    @Override
    public ServiceUserCertificate getCertificate(GetCertificateRequest getCertificateRequest) {
        Document document = fetchXml(String.format("https://fasit.adeo.no/conf/resources?envClass=%s&type=Certificate&alias=%s&bestmatch=true", getCertificateRequest.environmentClass, getCertificateRequest.alias));

        String alias = extractStringProperty(document, "keystorealias");
        String password = fetchJson(extractStringProperty(document, "keystorepassword"));
        byte[] keystore = fetchBytes(extractStringProperty(document, "keystore"));

        return new ServiceUserCertificate()
                .setKeystorealias(alias)
                .setKeystorepassword(password)
                .setKeystore(keystore);
    }

    @Override
    public List<RestService> getRestServices(String alias) {
        return httpClient(client -> client.target("https://fasit.adeo.no/api/v2/resources")
                .queryParam("type", "RestService")
                .queryParam("alias", alias)
                .queryParam("usage", true)
                .request()
                .get(RestServiceDTO.LIST_TYPE)
                .stream()
                .map(dto -> RestService.builder()
                        .alias(dto.getAlias())
                        .url(dto.getUrl())
                        .application(dto.getApplication())
                        .environment(dto.getEnvironment())
                        .environmentClass(dto.getEnvironmentClass())
                        .build()
                ).collect(Collectors.toList())
        );
    }

    @Override
    public WebServiceEndpoint getWebServiceEndpoint(String alias, String environment) {
        return httpClient(client -> client.target("https://fasit.adeo.no/api/v2/resources")
                .queryParam("type", "WebServiceEndpoint")
                .queryParam("environment", environment)
                .queryParam("alias", alias)
                .queryParam("usage", true)
                .request()
                .get(WebServiceEndpointDTO.LIST_TYPE)
                .stream()
                .findFirst()
                .map(dto -> new WebServiceEndpoint()
                        .setUrl(dto.properties.endpointUrl)
                )
                .orElseThrow(() -> new IllegalStateException(String.format("fant ikke '%s' i environment '%s'",
                        alias,
                        environment

                )))
        );
    }

    @SneakyThrows
    @Override
    public ApplicationConfig getApplicationConfig(GetApplicationConfigRequest getApplicationConfigRequest) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        JsonNode jsonNode = objectMapper.readTree(fetchJson(format("https://fasit.adeo.no/conf/environments/%s/applications/%s", getApplicationConfigRequest.environment, getApplicationConfigRequest.applicationName)));
        JsonNode cluster = jsonNode.get("cluster");
        applicationConfig.domain = cluster.get("domain").textValue();

        JsonNode nodes = cluster.get("nodes");
        for (JsonNode node : nodes) {
            applicationConfig.hostname = node.get("hostname").textValue();
            applicationConfig.deployerUsername = node.get("username").textValue();
            applicationConfig.deployerPasswordUrl = node.get("passwordRef").textValue();
        }

        LOG.info("{} = {}", getApplicationConfigRequest.applicationName, applicationConfig);
        return applicationConfig;
    }

    @Override
    public List<LoadBalancerConfig> getLoadbalancerConfig(String alias) {
        return httpClient(client -> client.target("https://fasit.adeo.no/api/v2/resources")
                .queryParam("type", "LoadBalancerConfig")
                .queryParam("alias", alias)
                .queryParam("usage", true)
                .request()
                .get(LoadBalancerConfigDTO.LIST_TYPE)
                .stream()
                .map(dto -> new LoadBalancerConfig()
                        .setContextRoots(dto.properties.contextRoots)
                        .setUrl(dto.properties.url)
                        .setEnvironment(dto.scope.environment)
                ).collect(Collectors.toList())
        );
    }

    @Override
    public LdapConfig getLdapConfig(String environmentClass) {
        return httpClient(client -> client.target("https://fasit.adeo.no/api/v2/resources")
                .queryParam("type", "LDAP")
                .queryParam("alias", "ldap")
                .queryParam("usage", true)
                .request()
                .get(LDAPDTO.LIST_TYPE)
                .stream()
                .filter(p -> environmentClass.equals(p.scope.environmentclass) && "ldap".equals(p.alias) && "fss".equals(p.scope.zone))
                .findFirst()
                .map(dto -> new LdapConfig()
                        .setUrl(dto.properties.url)
                        .setBaseDN(dto.properties.basedn)
                        .setUsername(dto.properties.username)
                        .setPassword(getPassword(dto.secrets.password.ref))
                )
                .orElseThrow(() -> new IllegalStateException("fant ikke ldap i environmentClass: " + environmentClass))
        );
    }

    @Override
    public Properties getApplicationProperties(GetApplicationPropertiesRequest getApplicationPropertiesRequest) {
        return httpClient(client -> client.target("https://fasit.adeo.no/api/v2/resources")
                .queryParam("type", "ApplicationProperties")
                .queryParam("alias", getApplicationPropertiesRequest.alias)
                .queryParam("environmentclass", getApplicationPropertiesRequest.environmentClass)
                .queryParam("usage", true)
                .request()
                .get(ApplicationPropertiesDTO.LIST_TYPE)
                .stream()
                .findFirst()
                .map(dto -> stringToProperties(dto.properties.applicationProperties))
                .orElseThrow(() -> new IllegalStateException("fant ikke application-properties for følgende parametre: " + getApplicationPropertiesRequest))
        );
    }

    @Override
    @SneakyThrows
    public Properties getApplicationEnvironment(GetApplicationEnvironmentRequest getApplicationEnvironmentRequest) {
        ApplicationConfig applicationConfig = getApplicationConfig(GetApplicationConfigRequest.builder()
                .environment(getApplicationEnvironmentRequest.environment)
                .applicationName(getApplicationEnvironmentRequest.applicationName)
                .build());

        JSch.setLogger(new com.jcraft.jsch.Logger() {
            @Override
            public boolean isEnabled(int level) {
                return true;
            }

            @Override
            public void log(int level, String message) {
                LOG.info(message);
            }
        });

        JSch jsch = new JSch();
        String hostname = applicationConfig.hostname;
        Session session = jsch.getSession(applicationConfig.deployerUsername, hostname);
        try {
            session.setPassword(getPassword(applicationConfig.deployerPasswordUrl));

            session.setConfig("StrictHostKeyChecking", FALSE.toString());
            session.setConfig("PreferredAuthentications", "password");
            session.connect();

            ChannelExec shell = (ChannelExec) session.openChannel("exec");
            try {
                String command = String.format("sudo cat /app/%s/configuration/environment.properties", getApplicationEnvironmentRequest.applicationName);
                shell.setCommand(command);
                shell.setErrStream(System.err);
                LOG.info("connecting...");
                shell.connect();
                int attempt = 0;
                while (!shell.isConnected() && attempt++ < 5) {
                    Thread.sleep(100L);
                }
                if (!shell.isConnected()) {
                    throw new IllegalStateException();
                }
                LOG.info("connected!");
                Properties properties = new Properties();
                properties.load(shell.getInputStream());
                if (properties.isEmpty()) {
                    throw new IllegalStateException(String.format("[%s] mot %s gav ingen properties", command, hostname));
                }
                return properties;
            } finally {
                shell.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }

    private static Document fetchXml(String resourceUrl) {
        LOG.info("Fetching xml: {}", resourceUrl);
        return httpClient(httpClient -> {
            Response contentResponse = httpClient
                    .target(resourceUrl)
                    .request()
                    .get();

            String resourceXml = getContent(contentResponse);

            LOG.info(resourceXml);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new InputSource(new StringReader(resourceXml)));
        });
    }

    @SneakyThrows
    public static <T> T httpClient(With<Client, T> httpClientConsumer) {
        int attempt = 0;
        do {
            try {
                return invokeHttpClient(httpClientConsumer);
            } catch (Throwable throwable) {
                if (throwable instanceof IllegalStateException || throwable instanceof NotAuthorizedException) {
                    throw throwable;
                }
                LOG.warn("feil mot fasit");
                LOG.warn(throwable.getMessage(), throwable);
                Thread.sleep(5000L);
            }
        } while (attempt++ < 5);
        throw new IllegalStateException("Klarer ikke å snakke med Fasit");
    }

    private static <T> T invokeHttpClient(With<Client, T> httpClientConsumer) {
        return RestUtils.withClient(RestUtils.DEFUALT_CLIENT_FILTER_CONFIG.withDisableMetrics(true),(httpClient) -> {
            httpClient.register(HttpAuthenticationFeature.basic(getFasitUser(), getFasitPassword()));
            return httpClientConsumer.withSafe(httpClient);
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

    private static FasitUtils.UsernameAndPassword getUsernameAndPassword(String resourceUrl) {
        return getUsernameAndPassword(fetchXml(resourceUrl));
    }

    private static FasitUtils.UsernameAndPassword getUsernameAndPassword(Document document) {
        FasitUtils.UsernameAndPassword usernameAndPassword = new FasitUtils.UsernameAndPassword();
        usernameAndPassword.setUsername(extractStringProperty(document, "username"));

        String passwordUrl = extractStringProperty(document, "password");
        usernameAndPassword.setPassword(getPassword(passwordUrl));
        return usernameAndPassword;
    }

    private static String getPassword(String passwordUrl) {
        LOG.info("fetching password from: {}", passwordUrl);
        return httpClient(httpClient -> getContent(httpClient
                .target(passwordUrl)
                .request()
                .get()
        ));
    }


    private static String getContent(Response contentResponse) {
        String contentAsString = contentResponse.readEntity(String.class);
        if (contentResponse.getStatus() != 200) {
            throw new IllegalStateException(contentAsString);
        } else {
            return contentAsString;
        }
    }

    private static String fetchJson(String url) {
        LOG.info("Fetching json: {}", url);
        String json = httpClient(httpClient -> httpClient.target(url).request(APPLICATION_JSON).get(String.class));
        LOG.info(json.replaceAll("\n", ""));
        return json;
    }

    private static byte[] fetchBytes(String url) {
        return httpClient(httpClient -> httpClient.target(url).request().get(byte[].class));
    }

    @SneakyThrows
    private Properties stringToProperties(String string) {
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(string.getBytes()));
        return properties;
    }

    @SneakyThrows
    private static <T> T fetchJsonObject(String url, Class<T> type) {
        return objectMapper.readValue(fetchJson(url), type);
    }

    @SneakyThrows
    private static <T> List<T> fetchJsonObjects(String url, Class<T> type) {
        return objectMapper.readValue(fetchJson(url), TypeFactory.defaultInstance().constructCollectionType(List.class, type));
    }

    @FunctionalInterface
    public interface With<T, R> {

        @SneakyThrows
        default R withSafe(T t){
            return with(t);
        }

        R with(T t) throws Exception;

    }


}
