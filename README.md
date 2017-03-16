Setup for å bruke biblioteket:

- Legg til `@Import` av `no.nav.sbl.dialogarena.common.abac.pep.context.ServiceContext`.
- For kjøring lokalt, legg til parameter for LDAP i lokal environmentfile. Se for eksempel http://stash.devillo.no/projects/SYFO/repos/moteadmin/browse/app/src/test/resources/environment.properties.
- Under `resources` i app-config.xml, legg til `<ldap alias="ldap" mapToProperty="ldap"/>`.
- Under `resources` i app-config.xml, legg til `<rest alias="abac.pdp.endpoint" mapToProperty="abac.endpoint" />`.
- I app-config.xml, legg til `<abacSecurity serviceUserResourceAlias="NAVN_PÅ_SERVICEBRUKER"/>`, hvor `NAVN_PÅ_SERVICEBRUKER` er navn på servicebrukeren for applikasjonen din slik det er definert i applikasjonens Credential-ressurs i Fasit.
- Legg til property `role` i lokal properties file i Fasit. Akkurat nå heter denne `0000-GA-Modia-Oppfolging` men det kan komme å endres.
