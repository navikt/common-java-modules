[![Maven Central](https://img.shields.io/maven-central/v/no.nav.common/bom.svg)](https://search.maven.org/search?q=g:no.nav.common) ![Build Status](https://github.com/navikt/common-java-modules/workflows/Test,%20build%20and%20publish/badge.svg)

# NAV common Java modules

These libraries are in use by many applications in NAV.

## Building the libraries

In order to build `common-java-modules`, run from the root directory:

```
mvn clean install
```

Currently, all tests do not run outside of NAV's internal network, because of dependencies on internal resources. In order to run tests without network or on an external build server, use:

```
mvn clean install -Plokal
```



## Retrieving Git history

The pre-merge Git history may appear lost, but it is possible to do `git blame`
on specific files to see who worked on that file last.

--------

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact
regarding a specific submodule.
