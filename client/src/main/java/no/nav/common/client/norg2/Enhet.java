package no.nav.common.client.norg2;

import lombok.Value;

@Value
public class Enhet {
    long enhetId;
    String navn;
    String enhetNr;
    int antallRessurser;

    String status;
    String orgNivaa;
    String type;
    String organisasjonsnummer;

    String underEtableringDato;
    String aktiveringsdato;
    String underAvviklingDato;
    String nedleggelsesdato;

    boolean oppgavebehandler;
    int versjon;
    String sosialeTjenester;
    String kanalstrategi;
    String orgNrTilKommunaltNavKontor;
}
