package no.nav.apiapp.rest;

import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class AlltidJsonFilter implements WriterInterceptor, ReaderInterceptor {

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        context.setMediaType(APPLICATION_JSON_TYPE);
        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        context.setMediaType(APPLICATION_JSON_TYPE);
        context.proceed();
    }

}
