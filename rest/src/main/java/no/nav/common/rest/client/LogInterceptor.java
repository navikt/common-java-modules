package no.nav.common.rest.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.log.LogFilter;
import no.nav.common.log.MDCConstants;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.MDC;

import java.io.IOException;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static no.nav.common.log.LogFilter.NAV_CALL_ID_HEADER_NAMES;
import static no.nav.common.utils.EnvironmentUtils.getApplicationName;
import static no.nav.common.utils.StringUtils.of;

@Slf4j
public class LogInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();

        of(MDC.get(MDCConstants.MDC_CALL_ID))
                .ifPresent(callId -> stream(NAV_CALL_ID_HEADER_NAMES)
                .forEach(headerName-> requestBuilder.header(headerName, callId)));

        getApplicationName().ifPresent(applicationName -> requestBuilder.header(LogFilter.CONSUMER_ID_HEADER_NAME, applicationName));

        Request request = requestBuilder
                .method(original.method(), original.body())
                .build();

        try {
            Response response = chain.proceed(request);
            log.info(format("%d %s %s", response.code(), request.method(), request.url()));
            return response;
        } catch (Exception exception) {
            log.error(format("Request failed: %s %s", request.method(), request.url()), exception);
            throw exception;
        }
    }

}
