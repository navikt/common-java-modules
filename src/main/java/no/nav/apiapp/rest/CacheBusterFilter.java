package no.nav.apiapp.rest;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Date;

public class CacheBusterFilter implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        MultivaluedMap<String, Object> headers = context.getHeaders();
        headers.putSingle("Cache-Control", "no-cache");
        headers.putSingle("Pragma", "no-cache");
        headers.putSingle("Expires", new Date(0));
        context.proceed();
    }

}
