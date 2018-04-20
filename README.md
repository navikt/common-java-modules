# NAV common Java modules

These libraries are in use by many applications in NAV.

## Building the libraries

For the time being, you need to install
[the sanitized fork of modig-testcertificates](https://github.com/navikt/modig-testcertificates-safe-fork),
otherwise you will not be able to build.

```
git clone https://github.com/navikt/modig-testcertificates-safe-fork
cd modig-testcertificates-safe-fork
mvn clean install
```

In order to build `common-java-modules`, run from the root directory:

```
mvn clean install
```

Currently, they do not build outside of NAV's internal network, because of dependencies that are
not yet open source. However, our end goal is to be able to release these libraries to Maven Central.

## Retrieving Git history

The pre-merge Git history may appear lost, but it is possible to do `git blame`
on specific files to see who worked on that file last.

--------

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact
regarding a specific submodule.
