package no.nav.apiapp.version;

import lombok.SneakyThrows;
import no.nav.apiapp.ApiApplication;
import no.nav.sbl.util.EnvironmentUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class VersionService {

    static final String UNKNOWN_VERSION = "?";

    public List<Version> getVersions() {
        return Arrays.asList(
                version("java", EnvironmentUtils.getRequiredProperty("java.version")),
                version("common", ApiApplication.class),
                version("jetty", org.eclipse.jetty.util.Jetty.VERSION),
                version("spring", org.springframework.core.Constants.class),
                version("logback", ch.qos.logback.core.Context.class),
                version("cxf", org.apache.cxf.Bus.class),
                version("jersey", org.glassfish.jersey.CommonProperties.class),
                version("jose4j", org.jose4j.jwt.JwtClaims.class),
                version("ehcache", net.sf.ehcache.Ehcache.class),
                version("micrometer", io.micrometer.core.instrument.Meter.class),
                version("jackson", com.fasterxml.jackson.core.JsonFactory.class)
        );
    }

    private Version version(String spring, Class<?> aClass) {
        return version(spring, resolveManifestVersion(aClass));
    }

    private Version version(String component, String version) {
        return Version.builder()
                .component(component)
                .version(version)
                .build();
    }

    @SneakyThrows
    private String resolveManifestVersion(Class<?> applicationContextClass) {
        Package classPackage = applicationContextClass.getPackage();
        String implementationVersion = classPackage.getImplementationVersion();
        if (implementationVersion != null) {
            return implementationVersion;
        } else {
            URL jarUrl = applicationContextClass.getProtectionDomain().getCodeSource().getLocation();
            try (InputStream inputStream = jarUrl.openStream()) {
                try (JarInputStream jarStream = new JarInputStream(inputStream)) {
                    Manifest manifest = jarStream.getManifest();
                    if (manifest != null) {
                        return manifest.getMainAttributes()
                                .entrySet()
                                .stream()
                                .filter(e -> new Attributes.Name("Bundle-Version").equals(e.getKey()))
                                .map(Map.Entry::getValue)
                                .flatMap(this::attributeValuetoString)
                                .findFirst()
                                .orElse(UNKNOWN_VERSION);
                    }
                }
            }
            return UNKNOWN_VERSION;
        }

    }

    private Stream<String> attributeValuetoString(Object versionAttribute) {
        return ofNullable(versionAttribute)
                .map(Object::toString)
                .map(Stream::of)
                .orElseGet(Stream::empty);
    }

}
