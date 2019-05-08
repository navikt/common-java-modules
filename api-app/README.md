# API-APP

Bibliotek som hjelper til å sette opp konsistente applikasjoner som eksponerer et api (REST eller SOAP)


### 1) Grunnleggende versjonering

Tas i bruk ved å legge til følgende i applikasjonens parent-pom:
```
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>no.nav.common</groupId>
                <artifactId>api-app-bom</artifactId>
                <version>RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            ...
           
```
Dette importer en bill-of-material som tilbyr et konsistent versjonert sett av avhengigheter som spring, jersey, jackson, common-cxf, modig-security, common-jetty m.m.
Dermed kan


### 2) Oppsett av REST-api

Hvis ønskelig, kan api-app sette opp et rest-api og eventuelle soap-tjenester med fornuftige defaults basert på en annotert klasse i prosjektet. 

Legg til følgende avhengighet i war-modulen:
```
    <dependency>
        <groupId>no.nav.common</groupId>
        <artifactId>api-app</artifactId>
    </dependency>
```
Api-app settes da opp automatisk på applikasjonsservere som ufører scanning etter servlet 3.0-annotasjoner, og vil logge retningslinjer for resterende konfigurasjon ved oppstart. 

Hvis ikke annotasjonsscanning brukes, legg til følgende i web.xml: 
```
    <listener>
        <listener-class>no.nav.apiapp.ApiAppServletContextListener</listener-class>
    </listener>
```

### 3) Konfigurering av HTTP-headere

Som default returnerer api-app headere som slår av all caching på klientsiden:

```
Cache-Control: no-cache no-store must-revalidate
Pragma: no-cache
```

For å tillate lagring av data på klienten kan følgende miljøvariabel settes i applikasjonen:

`ALLOW_CLIENT_STORAGE=true`

Cache-Control-headeren vil da se slik ut: 

`Cache-Control: no-cache`

Man kan også fjerne Pragma-headeren ved å definere `DISABLE_PRAGMA_HEADER=true`