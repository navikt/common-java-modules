package no.nav.common.nais;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.SneakyThrows;
import no.nav.common.utils.NaisUtils;
import no.nav.common.yaml.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static java.util.Collections.emptyList;

public class NaisYamlUtils {
    public static class NaiseratorSpec {
        public String apiVersion;
        public String kind;
        public Metadata metadata;
        public Spec spec;

        public static class Metadata {
            public String name;
            public String namespace;
            public Map<String, String> labels;
        }

        public static class Strategy {
            public String type;
        }

        public static class LivenessEndpoint {
            public String path;
            public String port;
            public Integer initialDelay;
            public Integer timeout;
            public Integer periodSeconds;
            public Integer failureThreshold;
        }

        public static class ReadinessEndpoint {
            public String path;
            public String port;
            public Integer initialDelay;
            public Integer timeout;
        }

        public static class Prometheus {
            public Boolean enabled;
            public String path;
        }

        public static class ResourceLimits {
            public String cpu;
            public String memory;
        }

        public static class Resources {
            public ResourceLimits requests;
            public ResourceLimits limits;
        }

        public static class Replicas {
            public Integer min;
            public Integer max;
            public Integer cpuThresholdPercentage;
        }

        public static class VaultPath {
            public String kvPath;
            public String mountPath;
        }

        public static class Vault {
            public Boolean enabled;
            public Boolean sidecar;
            public List<VaultPath> paths;
        }

        public static class FilesFrom {
            public String configmap;
            public String mountPath;
        }

        public static class EnvProperty {
            public String name;
            public String value;
        }

        public static class SecureLogs {
            public Boolean enabled;
        }

        public static class Service {
            public Integer port;
        }

        public static class Spec {
            public String image;
            public Integer port;
            public Strategy strategy;
            public LivenessEndpoint liveness;
            public ReadinessEndpoint readiness;
            public Prometheus prometheus;
            public Resources resources;
            public List<String> ingresses;
            public Replicas replicas;
            public Vault vault;
            public List<FilesFrom> filesFrom;
            public List<EnvProperty> env;
            public String preStopHookPath;
            public Boolean leaderElection;
            public Boolean webproxy;
            public String logformat;
            public String logtransform;
            public SecureLogs secureLogs;
            public Service service;
            public Boolean skipCaBundle;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NaisYamlUtils.class);

    public static NaiseratorSpec getConfig(String path) {
        String content = NaisUtils.getFileContent(path);
        return YamlUtils.fromYaml(content, NaiseratorSpec.class);
    }

    @SneakyThrows
    public static NaiseratorSpec getTemplatedConfig(String path, Object vars) {
        Handlebars handlebars = new Handlebars();
        Template template = handlebars.compileInline(NaisUtils.getFileContent(path));
        String content = template.apply(vars);
        return YamlUtils.fromYaml(content, NaiseratorSpec.class);
    }

    public static void loadFromYaml(String path, Properties target) {
        loadFromYaml(getConfig(path), target);
    }

    public static void loadFromYaml(NaiseratorSpec yaml, Properties target) {
        List<NaiseratorSpec.EnvProperty> env = Optional.ofNullable(yaml)
                .map((e) -> e.spec)
                .map((s) -> s.env)
                .orElse(emptyList());

        env.forEach((property) -> {
            String name = property.name;
            String value = property.value;

            if (target.containsKey(property.name)) {
                LOG.warn("Old value '{}' is replaced with", target.getProperty(name));
                LOG.warn("{} = {}", name, value);
            } else {
                LOG.info("Setting {} = '{}'", name, value);
            }

            target.setProperty(property.name, property.value);
        });
    }

    public static void loadFromYaml(String path) {
        loadFromYaml(path, System.getProperties());
    }

    public static void loadFromYaml(NaiseratorSpec yaml) {
        loadFromYaml(yaml, System.getProperties());
    }
}
