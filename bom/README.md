# BOM (bill-of-materials)

* se [maven-dokumentasjon for bom](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)

## Motivasjon
Det er utfordrende, særlig i store, komplekse applikasjoner, å versjonere tredjeparts-avhengigheter riktig. Selv "små" applikasjoner
har gjerne enorme avhengighetstre, jmf følgende applikasjonstre for en hello-world applikasjon for dagens nav-stack:

```

 no.nav.fo:veilarbdemo:war:1
 +- no.nav.sbl.dialogarena:api-app:jar:3.166.31:compile
 |  +- no.nav.modig:modig-log-filter:jar:1.1.3:compile
 |  |  \- no.nav.modig:modig-log-common:jar:1.1.3:compile
 |  +- no.nav.modig:modig-security-filter:jar:3.0.4:compile
 |  |  +- org.apache.httpcomponents:httpclient:jar:4.3.6:compile
 |  |  \- org.apache.httpcomponents:httpcore:jar:4.4:compile
 |  +- no.nav.modig:modig-security-core:jar:3.0.4:compile
 |  +- no.nav.dialogarena:oidc-security:jar:2017.09.21.15.29:compile
 |  |  +- no.nav.sbl.dialogarena:feed:jar:2017.09.21.15.29:compile
 |  |  |  \- org.quartz-scheduler:quartz:jar:2.2.1:compile
 |  |  |     \- c3p0:c3p0:jar:0.9.1.1:compile
 |  |  +- no.nav.sbl:rest:jar:2017.09.21.15.29:compile
 |  |  +- org.glassfish.jersey.core:jersey-client:jar:2.22.1:compile
 |  |  +- org.glassfish.jersey.media:jersey-media-json-jackson:jar:2.22.1:runtime
 |  |  |  +- org.glassfish.jersey.ext:jersey-entity-filtering:jar:2.22.1:runtime
 |  |  |  \- com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.5.4:compile
 |  |  +- org.json:json:jar:20160810:compile
 |  |  \- org.bitbucket.b_c:jose4j:jar:0.5.0:compile
 |  +- no.nav.sbl.dialogarena:common-abac:jar:2017.09.21.15.29:compile
 |  |  +- no.nav.abac.policies:abac-attribute-constants:jar:1.1.1-SNAPSHOT:compile
 |  |  +- com.google.code.gson:gson:jar:2.7:compile
 |  |  \- no.nav.sbl:json:jar:2017.09.21.15.29:compile
 |  +- no.nav:metrics:jar:2017.09.21.15.29:compile
 |  |  +- org.aspectj:aspectjrt:jar:1.8.8:compile
 |  |  +- org.aspectj:aspectjweaver:jar:1.8.8:runtime
 |  |  \- org.springframework:spring-aop:jar:4.3.4.RELEASE:compile
 |  +- no.nav.sbl.dialogarena:json:jar:1.1.3:compile
 |  |  +- com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.5.4:compile
 |  |  |  \- com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.5.4:compile
 |  |  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.5.4:compile
 |  |  \- com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.5.4:compile
 |  +- no.nav.sbl.dialogarena:common-web:jar:2017.09.21.15.29:compile
 |  |  \- joda-time:joda-time:jar:2.2:compile
 |  +- no.nav.sbl.dialogarena:common-types:jar:2017.09.21.15.29:compile
 |  +- no.nav.sbl.dialogarena:common-cxf:jar:2017.09.21.15.29:compile
 |  |  +- org.apache.cxf:cxf-rt-bindings-soap:jar:3.1.11:compile
 |  |  |  +- org.apache.cxf:cxf-rt-wsdl:jar:3.1.11:compile
 |  |  |  \- org.apache.cxf:cxf-rt-databinding-jaxb:jar:3.1.6:compile
 |  |  |     +- com.sun.xml.bind:jaxb-impl:jar:2.2.11:compile
 |  |  |     \- com.sun.xml.bind:jaxb-core:jar:2.2.11:compile
 |  |  +- org.apache.cxf:cxf-rt-frontend-simple:jar:3.1.11:compile
 |  |  +- no.nav.modig:modig-core:jar:1.0.1:compile
 |  |  +- org.jsoup:jsoup:jar:1.7.2:compile
 |  |  \- org.eclipse.jetty:jetty-servlet:jar:9.3.0.M2:compile
 |  +- no.nav.modig:modig-security-ws:jar:3.0.4:compile
 |  |  +- org.apache.wss4j:wss4j-ws-security-common:jar:2.1.5:compile
 |  |  |  +- org.apache.santuario:xmlsec:jar:2.0.6:compile
 |  |  |  +- org.opensaml:opensaml-saml-impl:jar:3.1.1:compile
 |  |  |  |  +- org.opensaml:opensaml-security-impl:jar:3.1.1:compile
 |  |  |  |  \- org.opensaml:opensaml-xmlsec-impl:jar:3.1.1:compile
 |  |  |  +- org.opensaml:opensaml-xacml-impl:jar:3.1.1:compile
 |  |  |  |  \- org.opensaml:opensaml-xacml-api:jar:3.1.1:compile
 |  |  |  +- org.opensaml:opensaml-xacml-saml-impl:jar:3.1.1:compile
 |  |  |  |  \- org.opensaml:opensaml-xacml-saml-api:jar:3.1.1:compile
 |  |  |  +- org.jasypt:jasypt:jar:1.9.2:compile
 |  |  |  \- org.apache.geronimo.specs:geronimo-javamail_1.4_spec:jar:1.7.1:compile
 |  |  +- org.apache.wss4j:wss4j-ws-security-dom:jar:2.1.9:compile
 |  |  \- org.opensaml:opensaml-saml-api:jar:3.1.1:compile
 |  |     +- org.opensaml:opensaml-xmlsec-api:jar:3.1.1:compile
 |  |     |  \- org.opensaml:opensaml-security-api:jar:3.1.1:compile
 |  |     |     +- org.cryptacular:cryptacular:jar:1.0:compile
 |  |     |     \- org.bouncycastle:bcprov-jdk15on:jar:1.51:compile
 |  |     +- org.opensaml:opensaml-soap-api:jar:3.1.1:compile
 |  |     +- org.opensaml:opensaml-messaging-api:jar:3.1.1:compile
 |  |     |  \- org.opensaml:opensaml-core:jar:3.1.1:compile
 |  |     +- org.opensaml:opensaml-profile-api:jar:3.1.1:compile
 |  |     +- org.opensaml:opensaml-storage-api:jar:3.1.1:compile
 |  |     \- net.shibboleth.utilities:java-support:jar:7.1.1:compile
 |  |        \- com.google.code.findbugs:jsr305:jar:3.0.0:compile
 |  +- org.springframework:spring-web:jar:4.3.4.RELEASE:compile
 |  +- javax.ws.rs:javax.ws.rs-api:jar:2.0.1:compile
 |  +- org.glassfish.jersey.ext:jersey-spring3:jar:2.22.1:runtime
 |  |  +- org.glassfish.hk2:hk2:jar:2.4.0-b31:runtime
 |  |  |  +- org.glassfish.hk2:config-types:jar:2.4.0-b31:runtime
 |  |  |  +- org.glassfish.hk2:hk2-core:jar:2.4.0-b31:runtime
 |  |  |  +- org.glassfish.hk2:hk2-config:jar:2.4.0-b31:runtime
 |  |  |  |  +- org.jvnet:tiger-types:jar:1.4:runtime
 |  |  |  |  \- org.glassfish.hk2.external:bean-validator:jar:2.4.0-b31:runtime
 |  |  |  +- org.glassfish.hk2:hk2-runlevel:jar:2.4.0-b31:runtime
 |  |  |  \- org.glassfish.hk2:class-model:jar:2.4.0-b31:runtime
 |  |  |     \- org.glassfish.hk2.external:asm-all-repackaged:jar:2.4.0-b31:runtime
 |  |  \- org.glassfish.hk2:spring-bridge:jar:2.4.0-b31:runtime
 |  +- org.glassfish.hk2:hk2-api:jar:2.4.0-b34:compile
 |  |  +- org.glassfish.hk2:hk2-utils:jar:2.4.0-b34:compile
 |  |  \- org.glassfish.hk2.external:aopalliance-repackaged:jar:2.4.0-b34:compile
 |  +- org.glassfish.jersey.containers:jersey-container-servlet-core:jar:2.22.1:compile
 |  |  \- org.glassfish.jersey.core:jersey-common:jar:2.22.1:compile
 |  |     +- org.glassfish.jersey.bundles.repackaged:jersey-guava:jar:2.22.1:compile
 |  |     \- org.glassfish.hk2:osgi-resource-locator:jar:1.0.1:compile
 |  +- org.glassfish.jersey.core:jersey-server:jar:2.22.1:compile
 |  |  +- org.glassfish.jersey.media:jersey-media-jaxb:jar:2.22.1:compile
 |  |  +- javax.annotation:javax.annotation-api:jar:1.2:compile
 |  |  +- org.glassfish.hk2:hk2-locator:jar:2.4.0-b31:compile
 |  |  |  \- org.javassist:javassist:jar:3.18.1-GA:compile
 |  |  \- javax.validation:validation-api:jar:1.1.0.Final:compile
 |  +- com.fasterxml.jackson.core:jackson-core:jar:2.5.4:compile
 |  +- org.webjars:swagger-ui:jar:2.2.10:runtime
 |  +- io.swagger:swagger-jaxrs:jar:1.5.13:compile
 |  |  +- org.reflections:reflections:jar:0.9.10:compile
 |  |  |  \- com.google.code.findbugs:annotations:jar:2.0.1:compile
 |  |  \- com.google.guava:guava:jar:20.0:compile
 |  +- org.springframework:spring-context:jar:4.3.4.RELEASE:compile
 |  |  \- org.springframework:spring-expression:jar:4.3.4.RELEASE:compile
 |  +- org.springframework:spring-beans:jar:4.3.4.RELEASE:compile
 |  +- org.apache.cxf:cxf-core:jar:3.1.11:compile
 |  |  +- org.codehaus.woodstox:woodstox-core-asl:jar:4.4.1:compile
 |  |  |  \- org.codehaus.woodstox:stax2-api:jar:3.1.4:compile
 |  |  \- org.apache.ws.xmlschema:xmlschema-core:jar:2.2.1:compile
 |  +- org.apache.cxf:cxf-rt-frontend-jaxws:jar:3.1.11:compile
 |  |  +- xml-resolver:xml-resolver:jar:1.2:compile
 |  |  +- org.ow2.asm:asm:jar:5.0.4:compile
 |  |  +- org.apache.cxf:cxf-rt-bindings-xml:jar:3.1.11:compile
 |  |  \- org.apache.cxf:cxf-rt-ws-addr:jar:3.1.11:compile
 |  +- org.apache.cxf:cxf-rt-transports-http:jar:3.1.11:compile
 |  +- no.nav.modig:modig-security-sts:jar:3.0.4:compile
 |  +- org.apache.cxf:cxf-rt-ws-security:jar:3.1.11:compile
 |  |  +- org.apache.cxf:cxf-rt-security-saml:jar:3.1.11:compile
 |  |  |  \- org.apache.cxf:cxf-rt-security:jar:3.1.11:compile
 |  |  +- net.sf.ehcache:ehcache:jar:2.10.3:compile
 |  |  +- org.apache.wss4j:wss4j-ws-security-stax:jar:2.1.9:compile
 |  |  |  \- org.apache.wss4j:wss4j-bindings:jar:2.1.9:compile
 |  |  \- org.apache.wss4j:wss4j-ws-security-policy-stax:jar:2.1.9:compile
 |  +- org.apache.wss4j:wss4j-policy:jar:2.1.9:compile
 |  +- org.apache.cxf:cxf-rt-ws-policy:jar:3.1.11:compile
 |  |  \- wsdl4j:wsdl4j:jar:1.6.3:compile
 |  +- org.apache.neethi:neethi:jar:3.0.3:compile
 |  +- org.apache.commons:commons-lang3:jar:3.5:compile
 |  +- commons-codec:commons-codec:jar:1.6:compile
 |  +- javax.inject:javax.inject:jar:1:compile
 |  +- org.glassfish.hk2.external:javax.inject:jar:2.4.0-b34:runtime
 |  +- org.springframework:spring-core:jar:4.3.4.RELEASE:compile
 |  +- io.swagger:swagger-core:jar:1.5.13:compile
 |  |  \- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.8.5:compile
 |  |     \- org.yaml:snakeyaml:jar:1.17:compile
 |  +- io.swagger:swagger-annotations:jar:1.5.13:compile
 |  +- io.swagger:swagger-models:jar:1.5.13:compile
 |  +- org.slf4j:slf4j-api:jar:1.7.21:compile
 |  +- org.slf4j:jcl-over-slf4j:jar:1.7.21:runtime
 |  +- ch.qos.logback:logback-classic:jar:1.2.3:runtime
 |  +- ch.qos.logback:logback-core:jar:1.2.3:runtime
 |  +- org.codehaus.janino:janino:jar:3.0.7:runtime
 |  |  \- org.codehaus.janino:commons-compiler:jar:3.0.7:runtime
 |  +- org.hamcrest:hamcrest-core:jar:1.3:compile
 |  +- com.fasterxml.jackson.core:jackson-databind:jar:2.5.4:compile
 |  +- org.eclipse.jetty:jetty-server:jar:9.3.0.M2:compile
 |  |  \- org.eclipse.jetty:jetty-io:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-webapp:jar:9.3.0.M2:compile
 |  |  \- org.eclipse.jetty:jetty-xml:jar:9.3.0.M2:compile
 |  \- commons-io:commons-io:jar:2.5:compile
 +- no.nav.sbl.dialogarena:common-jetty:jar:2017.09.21.15.29:compile
 |  +- net.sourceforge.collections:collections-generic:jar:4.01:compile
 |  +- org.eclipse.jetty:jetty-security:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-jaas:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-jaspi:jar:9.3.0.M2:compile
 |  |  \- org.eclipse.jetty.orbit:javax.security.auth.message:jar:1.0.0.v201108011116:compile
 |  +- org.eclipse.jetty:jetty-plus:jar:9.3.0.M2:compile
 |  |  \- org.eclipse.jetty:jetty-jndi:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-util:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-http:jar:9.3.0.M2:compile
 |  +- org.eclipse.jetty:jetty-annotations:jar:9.3.0.M2:compile
 |  |  \- org.ow2.asm:asm-commons:jar:5.0.1:compile
 |  |     \- org.ow2.asm:asm-tree:jar:5.0.1:compile
 |  +- javax.jms:jms:jar:1.1:compile
 |  +- org.springframework:spring-jms:jar:4.3.4.RELEASE:compile
 |  |  +- org.springframework:spring-messaging:jar:4.3.4.RELEASE:compile
 |  |  \- org.springframework:spring-tx:jar:4.3.4.RELEASE:compile
 |  +- org.springframework:spring-jdbc:jar:4.3.4.RELEASE:compile
 |  +- com.ibm.mq:mq-jms:jar:6.0:compile
 |  +- com.ibm:com.ibm.mq:jar:6.1:runtime
 |  |  +- javax.resource:connector:jar:1.0:runtime
 |  |  \- javax.transaction:jta:jar:1.0.1B:runtime
 |  \- com.ibm:dhbcore:jar:6.1:runtime
 +- javax.servlet:javax.servlet-api:jar:3.1.0:compile
 \- org.junit.platform:junit-platform-launcher:jar:1.0.0-M6:test
    \- org.junit.platform:junit-platform-engine:jar:1.0.0-M6:test
       +- org.junit.platform:junit-platform-commons:jar:1.0.0-M6:test
       \- org.opentest4j:opentest4j:jar:1.0.0-M3:test


```

En kompliserende faktor er at bruken maven-dependency-analyzer tvinger alle applikasjoner til å oppgi alle avhengigheter
som brukes i applikasjonskoden i appens pom.xml, selv om de hentes inn transitivt. Gitt f.eks. følgende avhengighetskjeder:

* minapp -> guava
* minapp -> swagger -> guava
* minapp -> common-et-eller-annet -> modig-security -> guava

Da er minapp nødt til å spesifisere en versjon av guava som forhåpentligvis er runtime-kompatibel med den versjonen swagger og modig-security.
Og siden dette ikke er tydelig compile-time, må man teste dette runtime.   
Og hver gang man rører versjonen til guava, swagger, common-et-eller-annet eller modig-security, må man på nytt forsikre seg om at man er kompatibel.
I en kompleks applikasjon kan en endring av enhver avhengigihet sin versjon påkreve full systemtest/regresjonstest  



### Men løser ikke java 9 dette da?
Nei, dessverre. Java 9 støtter (på nåværende tidspunkt ihvertfall) ikke å har flere, ulike versjoner av samme modul, 
så problemet er dermed fortsatt like stort, hvis ikke større.  

## Løsning
Løsningen er at noen løser dette kompliserte problemet en gang i dette prosjektet og at denne løsningen gjenbrukes i
applikasjoner og fellesbiblioteker. Hvis noen har sære behov som gjør at løsningen ikke lar seg gjenbruke, må man løse 
versjonerings-problemet selv.

 
* Applikasjoner kan delegere versjonering av vanlige fellesbiblioteker og tredjeparts-avhengigheter til dette prosjektet
ved følgende oppsett i prosjektets rot-pom:

```
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>no.nav.common</groupId>
                <artifactId>bom</artifactId>
                <version>RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```
* version=RELEASE kan brukes hvis man er interessert i fortløpende oppdateringer i et langsiktig utviklingsløp.
Ellers anbefales å bruke samme eksplisitte versjon som mor-pommen.



* I fellesbiblioteker håndteres versjonering spesielt for å kunne [release disse koordinert](http://stash.devillo.no/projects/COMMON/repos/release/browse). 
Da blir oppsettet slik:

```
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>no.nav.common</groupId>
                <artifactId>bom</artifactId>
                <version>${project.parent.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```