package no.nav.apiapp.feil;

import no.nav.apiapp.soap.SoapFeilMapper;
import no.nav.sbl.dialogarena.types.feil.Feil;
import no.nav.sbl.dialogarena.types.feil.FeilDTO;
import no.nav.sbl.dialogarena.types.feil.FeilType;
import org.apache.commons.codec.binary.Hex;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.xml.ws.soap.SOAPFaultException;
import java.security.SecureRandom;
import java.util.Optional;

import static no.nav.sbl.dialogarena.types.feil.FeilType.*;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.Q;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.T;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.isEnvironmentClass;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class FeilMapper {

    public static final String VIS_DETALJER_VED_FEIL = "VIS_DETALJER_VED_FEIL";
    private static final SecureRandom secureRandom = new SecureRandom();

    public static FeilDTO somFeilDTO(Throwable exception) {
        return somFeilDTO(exception, getType(exception));
    }

    public static FeilDTO somFeilDTO(Throwable exception, Feil.Type type) {
        return new FeilDTO(
                nyFeilId(),
                Optional.ofNullable(type).orElse(UKJENT).getName(),
                visDetaljer() ? finnDetaljer(exception) : null
        );
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
            return valueOfOptional(FeilType.class, ((SOAPFaultException) throwable).getFault().getFaultCodeAsName().getLocalName()).orElse(UKJENT);
        } else if (throwable instanceof IllegalArgumentException) {
            return UGYLDIG_REQUEST;
        } else if (throwable instanceof NotFoundException) {
            return FINNES_IKKE;
        } else if (throwable instanceof NotAuthorizedException) {
            return INGEN_TILGANG;
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

    public static boolean visDetaljer() {
        return getOptionalProperty(VIS_DETALJER_VED_FEIL).map(Boolean::parseBoolean).orElse(false)
                || isEnvironmentClass(T)
                || isEnvironmentClass(Q);
    }

}
