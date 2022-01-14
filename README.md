[![Maven Central](https://img.shields.io/maven-central/v/no.nav.common/bom.svg)](https://search.maven.org/search?q=g:no.nav.common) ![Build Status](https://github.com/navikt/common-java-modules/workflows/Test,%20build%20and%20publish/badge.svg)

# NAV common Java modules

These libraries are in use by many applications in NAV.

## Version 1
This version is tightly coupled to `api-app` and contains many modules that can be considered deprecated after the release of version 2.
This version will continue to live on the `master` branch until most users of `common-java-modules` has migrated over to version 2.
If possible, no new features should be added to version 1.

## Version 2
Version 2 removes all of the deprecated modules from version 1 (including `api-app`) and aims to provide framework agnostic modules that can be used in any application.

## Checking for new dependency versions

```shell
mvn versions:display-dependency-updates
```

## Building the libraries

In order to build `common-java-modules`, run from the root directory:

```shell
mvn clean install
```

Currently, all tests do not run outside of NAV's internal network, because of dependencies on internal resources. In order to run tests without network or on an external build server, use:

```shell
mvn clean install -Plokal
```

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact
regarding a specific submodule.
