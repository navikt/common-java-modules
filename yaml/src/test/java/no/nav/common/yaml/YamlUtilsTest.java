package no.nav.common.yaml;


import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlUtilsTest {


    @Test
    public void toFromYaml() {
        String yaml = "list: [\"a\",\"b\",\"c\"]\naBoolean: true";
        String yamlFormatted = "---\naBoolean: true\nlist:\n- \"a\"\n- \"b\"\n- \"c\"\n";

        TestObject testObject = YamlUtils.fromYaml(yaml, TestObject.class);
        assertThat(testObject.list).isEqualTo(Arrays.asList("a", "b", "c"));
        assertThat(testObject.aBoolean).isTrue();

        TestObject testObjectFormatted = YamlUtils.fromYaml(yamlFormatted, TestObject.class);
        assertThat(testObjectFormatted).isEqualTo(testObject);

        String serializedYaml = YamlUtils.toYaml(testObject);
        assertThat(serializedYaml).isEqualTo(yamlFormatted);
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode
    private static class TestObject {
        private boolean aBoolean;
        private List<String> list;
    }

}