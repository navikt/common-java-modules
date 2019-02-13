package no.nav.fasit;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import no.nav.fasit.client.FasitClient;
import no.nav.fasit.client.FasitClientImpl;
import no.nav.fasit.client.FasitClientMock;
import no.nav.fasit.dto.RestService;
import no.nav.sbl.util.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.ObjectUtils.isEqual;
import static org.slf4j.LoggerFactory.getLogger;


public class FasitUtils {

    public static final String FASIT_USERNAME_VARIABLE_NAME = "domenebrukernavn";
    public static final String FASIT_PASSWORD_VARIABLE_NAME = "domenepassord";
    public static final String DEFAULT_ENVIRONMENT_VARIABLE_NAME = "testmiljo";
    public static final String MOCK_VARIABLE_NAME = "mock";

    private static final File fasitPropertyFile = new File(System.getProperty("user.home"), "fasit.properties");

    private static final Logger LOG = getLogger(FasitUtils.class);

    public static final String WELL_KNOWN_APPLICATION_NAME = "fasit";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String TEST_LOCAL = "test.local";
    public static final String OERA_T_LOCAL = "oera-t.local";
    public static final String OERA_Q_LOCAL = "oera-q.local";
    public static final String PREPROD_LOCAL = "preprod.local";

    private static final Map<String, List<String>> domainsByEnvironmentClass = new HashMap<>();
    private static final Map<String, Zone> zoneByDomain = new HashMap<>();

    private static final List<String> T_DOMAINS = Arrays.asList(OERA_T_LOCAL, TEST_LOCAL);
    private static final List<String> Q_DOMAINS = Arrays.asList(OERA_Q_LOCAL, PREPROD_LOCAL);

    public static final String TEST_DATA_ALIAS = "test_data";

    static {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        domainsByEnvironmentClass.put("t", T_DOMAINS);
        domainsByEnvironmentClass.put("q", Q_DOMAINS);

        zoneByDomain.put(OERA_Q_LOCAL, Zone.SBS);
        zoneByDomain.put(OERA_T_LOCAL, Zone.SBS);
        zoneByDomain.put(PREPROD_LOCAL, Zone.FSS);
        zoneByDomain.put(TEST_LOCAL, Zone.FSS);
    }

    public static String getVariable(String variableName) {
        return ofNullable(System.getProperty(variableName, System.getenv(variableName)))
                .orElseGet(() -> getVariableFromPropertyFile(variableName));
    }

    private static String getVariableFromPropertyFile(String variableName) {
        if (usingMock()) {
            return variableName;
        }
        return getOptionalVariableFromPropertyFile(variableName).orElseThrow(() -> new IllegalStateException(format(
                "mangler variabel '%s'. \nDenne må settes som property, miljøvariabel eller i '%s'.\nEvt set variabelen %s=true for å bruke mock-verdier\n",
                variableName,
                fasitPropertyFile.getAbsolutePath()
                , MOCK_VARIABLE_NAME
        )));
    }

    private static Optional<String> getOptionalVariableFromPropertyFile(String variableName) {
        return of(fasitPropertyFile)
                .filter(File::exists)
                .map(Util::loadProperties)
                .map(fasitProperties -> fasitProperties.getProperty(variableName));
    }

    public static DbCredentials getDbCredentials(String applicationName) {
        return getDbCredentials(getDefaultTestEnvironment(), applicationName);
    }

    public static DbCredentials getDbCredentials(TestEnvironment testEnvironment, String applicationName) {
        return getFasitClient().getDbCredentials(FasitClient.GetDbCredentialsRequest.builder()
                .testEnvironment(testEnvironment)
                .applicationName(applicationName)
                .build()
        );
    }

    public static ServiceUserCertificate getServiceUserCertificate(String serviceUser, String environmentClass) {
        return getFasitClient().getCertificate(FasitClient.GetCertificateRequest.builder()
                .alias(serviceUser)
                .environmentClass(environmentClass)
                .build()
        );
    }

    @SneakyThrows
    public static ApplicationConfig getApplicationConfig(String applicationName, String environment) {
        return getFasitClient().getApplicationConfig(FasitClient.GetApplicationConfigRequest.builder()
                .applicationName(applicationName)
                .environment(environment)
                .build()
        );
    }

    public static TestUser getTestUser(String userAlias) {
        return getTestUser(userAlias, getDefaultEnvironment());
    }

    public static TestUser getTestUser(String userAlias, String environment) {
        ServiceUser serviceUser = getServiceUser(
                userAlias,
                WELL_KNOWN_APPLICATION_NAME,
                environment,
                TEST_LOCAL
        );
        return new TestUser()
                .setUsername(serviceUser.username)
                .setPassword(serviceUser.password);
    }

    public static String getBaseUrl(String baseUrlAlias) {
        return getBaseUrl(baseUrlAlias, getDefaultEnvironment());
    }

    public static String getBaseUrl(String baseUrlAlias, Zone zone) {
        return getBaseUrl(baseUrlAlias, getDefaultEnvironment(), getDefaultDomain(zone));
    }

    public static String getBaseUrl(String baseUrlAlias, String environment) {
        return getBaseUrl(baseUrlAlias, environment, getOeraLocal(environment));
    }

    public static String getBaseUrl(String baseUrlAlias, String environment, String domain) {
        return getBaseUrl(baseUrlAlias, environment, domain, "fasit");
    }

    public static String getBaseUrl(String baseUrlAlias, String environment, String domain, String application) {
        return getFasitClient().getBaseUrl(FasitClient.GetBaseUrlRequest.builder()
                .baseUrlAlias(baseUrlAlias)
                .environment(environment)
                .domain(domain)
                .application(application)
                .build()
        );
    }

    public static List<LoadBalancerConfig> getLoadbalancerConfig(String alias) {
        return getFasitClient().getLoadbalancerConfig(alias);
    }

    public static LoadBalancerConfig getLoadbalancerConfig(String alias, String environment) {
        return getLoadbalancerConfig(alias)
                .stream()
                .filter(c -> environment.equals(c.environment))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("fant ikke '%s' i environment '%s'",
                        alias,
                        environment
                )));
    }

    public static LdapConfig getLdapConfig() {
        return getLdapConfig(getDefaultEnvironmentClass());
    }

    public static LdapConfig getLdapConfig(String environmentClass) {
        return getFasitClient().getLdapConfig(environmentClass);
    }

    private static FasitClient getFasitClient() {
        return usingMock() ? new FasitClientMock() : new FasitClientImpl();
    }

    public static boolean usingMock() {
        return getOptionalVariableFromPropertyFile(MOCK_VARIABLE_NAME).map(Boolean::parseBoolean).orElse(false);
    }

    public static List<RestService> getRestServices(String alias) {
        return getFasitClient().getRestServices(alias);
    }

    public static RestService getRestService(String alias) {
        return bestMatch(getRestServices(alias));
    }

    public static RestService getRestService(String alias, String environment) {
        return getRestServices(alias)
                .stream()
                .filter(r -> isEqual(r.getEnvironment(), environment))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("did not find %s in %s", alias, environment)));
    }

    public static WebServiceEndpoint getWebServiceEndpoint(String alias) {
        return getWebServiceEndpoint(alias, getDefaultEnvironment());
    }

    public static WebServiceEndpoint getWebServiceEndpoint(String alias, String environment) {
        return getFasitClient().getWebServiceEndpoint(alias, environment);
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName) {
        return getServiceUser(userAlias, applicationName, getDefaultEnvironment());
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, TestEnvironment environment) {
        return getServiceUser(userAlias, applicationName, environment.toString());
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, Zone zone) {
        String defaultEnvironment = getDefaultEnvironment();
        return getServiceUser(userAlias, applicationName, defaultEnvironment, zone.getDomain(defaultEnvironment));
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, String environment) {
        return getServiceUser(userAlias, applicationName, environment, resolveDomain(applicationName, environment));
    }

    static String resolveDomain(String applicationName, String environment) {
        if (usingMock()) {
            return "mock";
        }
        ApplicationConfig applicationConfig = getApplicationConfig(applicationName, environment);
        String domain = applicationConfig.domain;
        List<String> domains = getDomains(environment);
        if (domains.contains(domain)) {
            return domain;
        } else {
            // nais-apper i sbs-sonen registeres under fss-domener (!)
            // så dermed følgende omvei for at dette skal bli riktig:
            Zone zone = zoneByDomain.get(domain);
            return zone.getDomain(environment);
        }
    }

    public static String getDefaultDomain(Zone zone) {
        return zone.getDomain(getDefaultEnvironment());
    }

    private static List<String> getDomains(String environment) {
        return domainsByEnvironmentClass.get(getEnvironmentClass(environment));
    }

    public static OpenAmConfig getOpenAmConfig() {
        return getOpenAmConfig(getDefaultEnvironment());
    }

    public static OpenAmConfig getOpenAmConfig(String environment) {
        OpenAmConfig openAmConfig = getFasitClient().getOpenAmConfig(environment);
        LOG.info("openAm: {}", openAmConfig);
        return openAmConfig;
    }

    public static ServiceUser getServiceUser(String userAlias, String applicationName, String environment, String domain) {
        return getFasitClient().getCredentials(FasitClient.GetCredentialsRequest.builder()
                .userAlias(userAlias)
                .applicationName(applicationName)
                .environment(environment)
                .domain(domain)
                .build()
        );
    }

    public static Optional<String> getTestDataProperty(String propertyName) {
        return ofNullable(getTestDataProperties().getProperty(propertyName)).filter(StringUtils::notNullOrEmpty);
    }

    public static Properties getTestDataProperties() {
        return getApplicationProperties(TEST_DATA_ALIAS);
    }

    public static Properties getApplicationProperties(String alias) {
        return bestMatch(getFasitClient().getApplicationProperties(FasitClient.GetApplicationPropertiesRequest.builder()
                .alias(alias)
                .environmentClass(getDefaultEnvironmentClass())
                .build()
        ));
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

    public static String getDefaultEnvironmentClass() {
        return getEnvironmentClass(getDefaultEnvironment());
    }

    public static TestEnvironment getDefaultTestEnvironment() {
        return usingMock() ? TestEnvironment.MOCK : TestEnvironment.valueOf(getDefaultEnvironment().toUpperCase());
    }

    public static String getEnvironmentClass(String environment) {
        return environment == null ? null : environment.substring(0, 1);
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
            case "m":
                return "mock";
            default:
                throw new IllegalStateException(environment);
        }
    }

    public static no.nav.fasit.Queue getQueue(String alias) {
        return bestMatch(getQueues(alias));
    }

    public static List<no.nav.fasit.Queue> getQueues(String alias) {
        return getQueues(FasitClient.GetQueueRequest.builder()
                .alias(alias)
                .environment(getDefaultEnvironment())
                .build()
        );
    }

    public static List<Queue> getQueues(FasitClient.GetQueueRequest getQueueRequest) {
        return getFasitClient().getQueue(getQueueRequest);
    }

    public static QueueManager getQueueManager(String alias) {
        return bestMatch(getQueueManagers(alias));
    }

    private static <T extends Scoped> T bestMatch(List<T> scoped) {
        String environment = getDefaultEnvironment();
        String environmentClass = getEnvironmentClass(environment);
        Stream<Predicate<T>> matchers = Stream.of(
                s -> isEqual(environment, s.getEnvironment()),
                s -> isEqual(environmentClass, s.getEnvironmentClass())
        );
        return matchers
                .flatMap(p -> scoped.stream().filter(p))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "no best match for environment=%s or environmentClass=%s among: %s",
                        environment,
                        environmentClass,
                        scoped
                )));
    }

    public static List<QueueManager> getQueueManagers(String alias) {
        return getQueueManagers(FasitClient.GetQueueManagerRequest.builder()
                .alias(alias)
                .environmentClass(getDefaultEnvironmentClass())
                .zone("fss")
                .build()
        );
    }

    public static List<QueueManager> getQueueManagers(FasitClient.GetQueueManagerRequest getQueueManagerRequest) {
        return getFasitClient().getQueueManager(getQueueManagerRequest);
    }

    public enum Zone {
        FSS,
        SBS;

        public String getDomain(String environment) {
            switch (this) {
                case FSS:
                    return getFSSLocal(environment);
                case SBS:
                    return getOeraLocal(environment);
                default:
                    throw new IllegalStateException(environment);
            }
        }
    }

    public static boolean erEksterntDomene(String domain) {
        return domain != null && domain.contains("oera");
    }


    public static Properties getApplicationEnvironment(String applicationName) {
        return getApplicationEnvironment(applicationName, getDefaultEnvironment());
    }

    @SneakyThrows
    public static Properties getApplicationEnvironment(String applicationName, String environment) {
        return getFasitClient().getApplicationEnvironment(FasitClient.GetApplicationEnvironmentRequest.builder()
                .applicationName(applicationName)
                .environment(environment)
                .build()
        );
    }

    @Data
    @Accessors(chain = true)
    public static class UsernameAndPassword {
        public String username;
        public String password;
    }

}
