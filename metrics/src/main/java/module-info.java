module no.nav.metrics {
    requires slf4j.api;
    requires micrometer.core;
    requires micrometer.registry.prometheus;
    requires no.nav.log;
    requires no.nav.util;
    requires org.apache.commons.lang3;
    requires org.json;
    requires org.aspectj.runtime;
    requires spring.context;
}
