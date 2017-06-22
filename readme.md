# Common web

## Changelog

### Versjon 5.0

Stor endring i selftestsidene for å forholde seg til standarden som er ønsket
av ATOM/AURA. Les mer om ønsket oppsett på [confluence](https://confluence.adeo.no/display/AURA/Selftest).

I hovedsak er det 2 store breaking changes:
* Pingable-klassen fra common-types har helt annet API
    * Trenger endepunkt, beskrivelse og info om hvor kritisk eventuell feil er
* Json og HTML varianten er slått sammen til 1 servlet som returnerer HTML/JSON
    basert på accept-header i request.
