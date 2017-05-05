package no.nav.fo.apiapp.soap;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.*;
import org.apache.commons.lang3.NotImplementedException;

@SoapTjeneste(SoapEksempel.TJENESTENAVN)
public class SoapEksempel implements Aktoer_v2PortType {

    public static final String TJENESTENAVN = "/eksempel";
    public static final String IDENT_FOR_UKJENT_FEIL = "IDENT_FOR_UKJENT_FEIL";
    public static final String IDENT_FOR_VERSJONSKONFLIKT = "IDENT_FOR_VERSJONSKONFLIKT";

    @Override
    public WSHentAktoerIdForIdentListeResponse hentAktoerIdForIdentListe(WSHentAktoerIdForIdentListeRequest hentAktoerIdForIdentListeRequest) {
        throw new NotImplementedException("");
    }

    @Override
    @SneakyThrows
    public WSHentAktoerIdForIdentResponse hentAktoerIdForIdent(WSHentAktoerIdForIdentRequest hentAktoerIdForIdentRequest) throws HentAktoerIdForIdentPersonIkkeFunnet {
        String ident = hentAktoerIdForIdentRequest.getIdent();
        switch (ident) {
            case IDENT_FOR_UKJENT_FEIL:
                throw new Throwable("ukjent runtimefeil");
            case IDENT_FOR_VERSJONSKONFLIKT:
                throw new VersjonsKonflikt();
            default:
                WSHentAktoerIdForIdentResponse wsHentAktoerIdForIdentResponse = new WSHentAktoerIdForIdentResponse();
                wsHentAktoerIdForIdentResponse.setAktoerId("aktor-" + ident);
                return wsHentAktoerIdForIdentResponse;
        }
    }

    @Override
    public WSHentIdentForAktoerIdListeResponse hentIdentForAktoerIdListe(WSHentIdentForAktoerIdListeRequest hentIdentForAktoerIdListeRequest) {
        throw new NotImplementedException("");
    }

    @Override
    public void ping() {

    }

    @Override
    public WSHentIdentForAktoerIdResponse hentIdentForAktoerId(WSHentIdentForAktoerIdRequest hentIdentForAktoerIdRequest) throws HentIdentForAktoerIdPersonIkkeFunnet {
        throw new NotImplementedException("");
    }

}
