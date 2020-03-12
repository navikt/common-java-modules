package no.nav.fo.apiapp.soap;

import lombok.SneakyThrows;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.feil.VersjonsKonflikt;
import org.apache.servicemix.examples.cxf.HelloWorld;

@SoapTjeneste(SoapEksempel.TJENESTENAVN)
public class SoapEksempel implements HelloWorld {

    public static final String TJENESTENAVN = "/eksempel";
    public static final String IDENT_FOR_UKJENT_FEIL = "IDENT_FOR_UKJENT_FEIL";
    public static final String IDENT_FOR_VERSJONSKONFLIKT = "IDENT_FOR_VERSJONSKONFLIKT";
    public static final String IDENT_FOR_NOSTET_KALL = "IDENT_FOR_NOSTET_KALL";

    @Override
    @SneakyThrows
    public String sayHi(String ident) {
        switch (ident) {
            case IDENT_FOR_UKJENT_FEIL:
                throw new Throwable("ukjent runtimefeil");
            case IDENT_FOR_VERSJONSKONFLIKT:
                throw new VersjonsKonflikt();
            case IDENT_FOR_NOSTET_KALL:
                return new CXFClient<>(HelloWorld.class)
                        // TODO
                        .address("https://service-gw-t6.test.local/")
                        .build()
                        .sayHi(IDENT_FOR_UKJENT_FEIL);
            default:
                return "hello " + ident;
        }
    }

}
