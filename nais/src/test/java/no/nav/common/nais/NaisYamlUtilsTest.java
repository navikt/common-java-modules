package no.nav.common.nais;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class NaisYamlUtilsTest {
    private static final String base = "src/test/resources/";

    @Test
    public void getConfig_handles_missing_data() {
        NaisYamlUtils.NaiseratorSpec config = NaisYamlUtils.getConfig(base + "min-nais-example.yaml");

        assertThat(config.apiVersion).isEqualTo("nais.io/v1alpha1");
        assertThat(config.kind).isEqualTo("Application");

        assertThat(config.metadata.name).isEqualTo("nais-testapp");
        assertThat(config.metadata.labels.get("team")).isEqualTo("aura");

        assertThat(config.spec.image).isEqualTo("navikt/nais-testapp:65.0.0");
    }

    @Test
    public void getConfig_loads_whole_yaml() {
        NaisYamlUtils.NaiseratorSpec config = NaisYamlUtils.getConfig(base + "max-nais-example.yaml");

        assertThat(config.apiVersion).isEqualTo("nais.io/v1alpha1");
        assertThat(config.kind).isEqualTo("Application");

        assertThat(config.metadata.name).isEqualTo("nais-testapp");
        assertThat(config.metadata.namespace).isEqualTo("default");
        assertThat(config.metadata.labels.get("team")).isEqualTo("aura");

        assertThat(config.spec.image).isEqualTo("navikt/nais-testapp:65.0.0");
        assertThat(config.spec.port).isEqualTo(8080);
        assertThat(config.spec.strategy.type).isEqualTo("RollingUpdate");

        assertThat(config.spec.liveness.path).isEqualTo("isalive");
        assertThat(config.spec.liveness.port).isEqualTo("http");
        assertThat(config.spec.liveness.initialDelay).isEqualTo(20);
        assertThat(config.spec.liveness.timeout).isEqualTo(1);
        assertThat(config.spec.liveness.periodSeconds).isEqualTo(5);
        assertThat(config.spec.liveness.failureThreshold).isEqualTo(10);

        assertThat(config.spec.readiness.path).isEqualTo("isready");
        assertThat(config.spec.readiness.port).isEqualTo("http");
        assertThat(config.spec.readiness.initialDelay).isEqualTo(20);
        assertThat(config.spec.readiness.timeout).isEqualTo(1);

        assertThat(config.spec.replicas.min).isEqualTo(2);
        assertThat(config.spec.replicas.max).isEqualTo(4);
        assertThat(config.spec.replicas.cpuThresholdPercentage).isEqualTo(50);

        assertThat(config.spec.prometheus.enabled).isEqualTo(false);
        assertThat(config.spec.prometheus.path).isEqualTo("/metrics");

        assertThat(config.spec.resources.requests.cpu).isEqualTo("200m");
        assertThat(config.spec.resources.requests.memory).isEqualTo("256Mi");
        assertThat(config.spec.resources.limits.cpu).isEqualTo("500m");
        assertThat(config.spec.resources.limits.memory).isEqualTo("512Mi");

        assertThat(config.spec.ingresses).hasSize(2);


        assertThat(config.spec.vault.enabled).isEqualTo(false);
        assertThat(config.spec.vault.sidecar).isEqualTo(false);
        assertThat(config.spec.vault.paths).hasSize(1);
        assertThat(config.spec.vault.paths.get(0).kvPath).isEqualTo("/kv/preprod/fss/application/namespace");
        assertThat(config.spec.vault.paths.get(0).mountPath).isEqualTo("/var/run/secrets/nais.io/vault");

        assertThat(config.spec.filesFrom).hasSize(1);
        assertThat(config.spec.filesFrom.get(0).configmap).isEqualTo("example_files_configmap");
        assertThat(config.spec.filesFrom.get(0).mountPath).isEqualTo("/var/run/configmaps");

        assertThat(config.spec.env).hasSize(2);
        assertThat(config.spec.env.get(0).name).isEqualTo("MY_CUSTOM_VAR");
        assertThat(config.spec.env.get(0).value).isEqualTo("some_value");
        assertThat(config.spec.env.get(1).name).isEqualTo("MY_CUSTOM_VAR3");
        assertThat(config.spec.env.get(1).value).isEqualTo("some_value3");

        assertThat(config.spec.preStopHookPath).isEqualTo("/stop");
        assertThat(config.spec.leaderElection).isEqualTo(false);
        assertThat(config.spec.webproxy).isEqualTo(false);
        assertThat(config.spec.logformat).isEqualTo("accesslog");
        assertThat(config.spec.logtransform).isEqualTo("http_loglevel");
        assertThat(config.spec.secureLogs.enabled).isEqualTo(false);
        assertThat(config.spec.service.port).isEqualTo(80);
        assertThat(config.spec.skipCaBundle).isEqualTo(false);
    }

    @Test
    public void loadFromYaml_sets_props_on_given_target() {
        Properties target = new Properties();
        NaisYamlUtils.loadFromYaml(base + "max-nais-example.yaml", target);

        assertThat(target).hasSize(2);
        assertThat(target.getProperty("MY_CUSTOM_VAR")).isEqualTo("some_value");
        assertThat(target.getProperty("MY_CUSTOM_VAR3")).isEqualTo("some_value3");
    }

    @Test
    public void loadFromYaml_handles_missing_env() {
        Properties target = new Properties();
        NaisYamlUtils.loadFromYaml(base + "min-nais-example.yaml", target);

        assertThat(target).hasSize(0);
    }

    @Test
    public void getTemplatedConfig_handles_null() {
        NaisYamlUtils.NaiseratorSpec config = NaisYamlUtils.getTemplatedConfig(
                base + "min-nais-templated-example.yaml",
                null
        );

        assertThat(config.metadata.namespace).isEqualTo("default");
        assertThat(config.spec.image).isNull();
        assertThat(config.spec.ingresses).isNull();
        assertThat(config.spec.env).isNull();
    }

    @Test
    public void getTemplatedConfig_interpoleates_all_values() {
        String namespace = "q0";
        String image = "no.nav.testimage";
        List<String> ingresses = asList("http://url.nav.no", "https://url.nav.no");
        Map<String, String> env = new HashMap<>();
        env.put("envprops1", "envvalue1");
        env.put("envprops2", "envvalue2");

        Map<String, Object> context = new HashMap<>();
        context.put("namespace", namespace);
        context.put("image", image);
        context.put("ingresses", ingresses);
        context.put("env", env);

        NaisYamlUtils.NaiseratorSpec config = NaisYamlUtils.getTemplatedConfig(
                base + "min-nais-templated-example.yaml",
                context
        );

        assertThat(config.metadata.namespace).isEqualTo(namespace);
        assertThat(config.spec.image).isEqualTo(image);
        assertThat(config.spec.ingresses).hasSize(ingresses.size());
        assertThat(config.spec.env).hasSize(env.size());
    }
}