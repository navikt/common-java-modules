# Common suspend

Modul som lar en få nedetidsfri deploys

## Hvordan ta i bruk

Dra inn i maven `pom.xml`. Om koden bare brukes i web.xml og man har analyze-dependencies i maven på, må man ha scope runtime for at det skal fungere.

```
<dependency>
    <groupId>no.nav.sbl.dialogarena</groupId>
    <artifactId>common-suspend</artifactId>
    <version>1.0.1</version>
    <scope>runtime</scope>
</dependency>
```


Dra inn Servlets+filter i `web.xml`. Her dras det inn en IsAlive-servlet, så slett en evt. eksisterende IsAlive. Evt. kan en eksisterende IsAlive implementere sjekking av suspend-servleten.

```
<servlet>
    <servlet-name>isAlive</servlet-name>
    <servlet-class>no.nav.sbl.dialogarena.common.suspend.IsAliveServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>isAlive</servlet-name>
    <url-pattern>/internal/isAlive</url-pattern>
</servlet-mapping>

<filter>
    <filter-name>basicAuthenticationFilter</filter-name>
    <filter-class>no.nav.sbl.dialogarena.common.suspend.BasicAuthenticationFilter</filter-class>
    <init-param>
        <param-name>authProperty</param-name>
        <param-value>suspender</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>basicAuthenticationFilter</filter-name>
    <url-pattern>/management/suspend</url-pattern>
</filter-mapping>

<servlet>
    <servlet-name>suspend</servlet-name>
    <servlet-class>no.nav.sbl.dialogarena.common.suspend.SuspendServlet</servlet-class>
    <init-param>
        <param-name>shutdownTimeMs</param-name>
        <param-value>3000</param-value>
    </init-param>
</servlet>
<servlet-mapping>
    <servlet-name>suspend</servlet-name>
    <url-pattern>/management/suspend</url-pattern>
</servlet-mapping>
```

Man kan droppe `init-params`. Da er defaultverdier de samme som vist:
```
authProperty: suspender
shutdownTimeMs: 3000
```

Og i `app-config.xml` må dette konfes. Se [Suspend - confluence](https://confluence.adeo.no/pages/viewpage.action?pageId=209463118)

```
<resources>
    ...
    <credential alias="suspender" />
</resources>
<loadBalancer isAlive="/mincontextroot/internal/isAlive"/>
<suspend url="/mincontextroot/management/suspend" credential="suspender" />
```

## Changelog

### Versjon 1.0
* Første versjon. 
* Støtter at IsAlive returnerer feil i det man får en suspend, så noden blir meldt ut av bigIp
* En timer som avgjør når noden anser seg som "klar" til å bli tatt ned