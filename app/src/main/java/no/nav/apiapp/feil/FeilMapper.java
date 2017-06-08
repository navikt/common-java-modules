package no.nav.apiapp.feil;

import no.nav.apiapp.Constants;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.feil.Feil.Type.UKJENT;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class FeilMapper {

    private static final Set<String> MILJO_MED_DETALJER = new HashSet<>(asList("t", "u", "q"));

    public static FeilDTO somFeilDTO(Throwable exception) {
        Feil.Type type = getType(exception);
        return new FeilDTO(UUID.randomUUID().toString(), type, visDetaljer() ? finnDetaljer(exception) : null);
    }

    public static Feil.Type getType(Throwable throwable) {
        if (throwable instanceof Feil) {
            return ((Feil) throwable).getType();
        } else if (throwable instanceof SOAPFaultException) {
            return valueOfOptional(Feil.Type.class, ((SOAPFaultException) throwable).getFault().getFaultCodeAsName().getLocalName()).orElse(UKJENT);
        } else {
            return UKJENT;
        }
    }

    private static String finnDetaljer(Throwable exception) {
        String stackTrace = getStackTrace(exception);
        if (exception instanceof SOAPFaultException) {
            return of((SOAPFaultException) exception)
                    .map(SOAPFaultException::getFault)
                    .map(SOAPFault::getDetail)
                    .map(Detail::getValue)
                    .orElse(stackTrace);
        } else {
            return stackTrace;
        }
    }

    private static boolean visDetaljer() {
        return ofNullable(System.getProperty(Constants.MILJO_PROPERTY_NAME)).map(MILJO_MED_DETALJER::contains).orElse(false);
    }

}
