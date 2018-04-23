package no.nav.common.yaml;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlUtilsTest {

    @Test
    public void fromYaml() {
        TestObject testObject = YamlUtils.fromYaml("list: [\"a\",\"b\",\"c\"]\naBoolean: true", TestObject.class);
        assertThat(testObject.list).isEqualTo(Arrays.asList("a", "b", "c"));
        assertThat(testObject.aBoolean).isTrue();
    }

    @SuppressWarnings("unused")
    private static class TestObject {
        private boolean aBoolean;
        private List<String> list;
    }

}