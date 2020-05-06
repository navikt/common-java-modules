package no.nav.common.rest;

import no.nav.common.log.LogFilter;
import no.nav.common.log.MDCConstants;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;

import static java.util.Arrays.stream;
import static no.nav.common.log.LogFilter.NAV_CALL_ID_HEADER_NAMES;
import static no.nav.common.utils.EnvironmentUtils.getApplicationName;
import static no.nav.common.utils.StringUtils.of;
import static org.slf4j.LoggerFactory.getLogger;

public class LogInterceptor implements Interceptor {

    private static final Logger LOG = getLogger(LogInterceptor.class);

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

        LOG.info("{} {}", original.method(), original.url());

        return chain.proceed(request);
    }

}
