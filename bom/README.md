# BOM (bill-of-materials)

* se [maven-dokumentasjon for bom](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)

## Motivasjon
Det er utfordrende, særlig i store, komplekse applikasjoner, å versjonere tredjeparts-avhengigheter riktig.
Selv "små" applikasjoner har gjerne enorme avhengighetstre. En kompliserende faktor er at bruken av maven-dependency-analyzer tvinger alle applikasjoner til å oppgi alle avhengigheter
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