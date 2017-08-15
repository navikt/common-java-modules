package no.nav.apiapp.feil;

import no.nav.apiapp.Constants;
import no.nav.apiapp.soap.SoapFeilMapper;
import org.apache.commons.codec.binary.Hex;

import javax.ws.rs.NotFoundException;
import javax.xml.ws.soap.SOAPFaultException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.feil.Feil.Type.FINNES_IKKE;
import static no.nav.apiapp.feil.Feil.Type.UGYLDIG_REQUEST;
import static no.nav.apiapp.feil.Feil.Type.UKJENT;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class FeilMapper {

    private static final Set<String> MILJO_MED_DETALJER = new HashSet<>(asList("t", "u", "q"));
    private static final SecureRandom secureRandom = new SecureRandom();

    public static FeilDTO somFeilDTO(Throwable exception) {
        return somFeilDTO(exception, getType(exception));
    }

    public static FeilDTO somFeilDTO(Throwable exception, Feil.Type type) {
        return new FeilDTO(nyFeilId(), type, visDetaljer() ? finnDetaljer(exception) : null);
    }

    static String nyFeilId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }

    public static Feil.Type getType(Throwable throwable) {
        if (throwable instanceof Feil) {
            return ((Feil) throwable).getType();
        } else if (throwable instanceof SOAPFaultException) {
            return valueOfOptional(Feil.Type.class, ((SOAPFaultException) throwable).getFault().getFaultCodeAsName().getLocalName()).orElse(UKJENT);
        } else if (throwable instanceof IllegalArgumentException) {
            return UGYLDIG_REQUEST;
        } else if (throwable instanceof NotFoundException) {
            return FINNES_IKKE;
        } else {
            return UKJENT;
        }
    }

    private static FeilDTO.Detaljer finnDetaljer(Throwable exception) {
        return new FeilDTO.Detaljer(exception.getClass().getName(), exception.getMessage(), finnStackTrace(exception));
    }

    private static String finnStackTrace(Throwable exception) {
        String stackTrace = getStackTrace(exception);
        if (exception instanceof SOAPFaultException) {
            return SoapFeilMapper.finnStackTrace((SOAPFaultException) exception);
        } else {
            return stackTrace;
        }
    }

    private static boolean visDetaljer() {
        return ofNullable(System.getProperty(Constants.MILJO_PROPERTY_NAME)).map(MILJO_MED_DETALJER::contains).orElse(false);
    }

}
