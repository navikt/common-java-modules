Setup for å bruke biblioteket:

- Legge til @Import av ServiceContext
- For kjøring lokalt legge til parametrar for LDAP i lokal environmentfile. Se f eks http://stash.devillo.no/projects/SYFO/repos/moteadmin/browse/app/src/test/resources/environment.properties
- Under resources i appconfig legg til         <ldap alias="ldap" mapToProperty="ldap"/>
