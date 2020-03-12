package no.nav.apiapp.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;

import static no.nav.sbl.dialogarena.types.feil.FeilType.UGYLDIG_REQUEST;


public class ReadExceptionHandler implements ReaderInterceptor {

    private final ExceptionMapper exceptionMapper;

    public ReadExceptionHandler(ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        try {
            return context.proceed();
        } catch (Throwable e) {
            throw new WebApplicationException(exceptionMapper.toResponse(e, UGYLDIG_REQUEST));
        }
    }

}
