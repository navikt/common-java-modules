module no.nav.log {
    requires javax.servlet.api;
    requires spring.web;
    requires spring.context;
    requires no.nav.util;
    requires logback.classic;
    requires slf4j.api;
    requires logback.core;
    requires logstash.logback.encoder;

    exports no.nav.log;
    exports no.nav.log.sbl;
}
