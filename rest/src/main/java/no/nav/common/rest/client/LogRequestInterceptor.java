package no.nav.common.rest.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.log.MDCConstants;
import no.nav.common.utils.IdUtils;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.MDC;

import java.io.IOException;

import static java.lang.String.format;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;
import static no.nav.common.utils.EnvironmentUtils.getApplicationName;
import static no.nav.common.utils.StringUtils.of;

@Slf4j
public class LogRequestInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();

        of(MDC.get(MDCConstants.MDC_CALL_ID))
                .or(() -> of(MDC.get(MDCConstants.MDC_JOB_ID)))
                .or(() -> of(IdUtils.generateId())) // Generate a new call-id if it is missing from the MDC context
                .ifPresent(callId -> requestBuilder.header(NAV_CALL_ID_HEADER_NAME, callId));

        getApplicationName().ifPresent(applicationName -> requestBuilder.header(NAV_CONSUMER_ID_HEADER_NAME, applicationName));

        Request request = requestBuilder
                .method(original.method(), original.body())
                .build();

        String url = toStringWithoutQuery(request.url());
        long requestStarted = System.currentTimeMillis();

        try {
            Response response = chain.proceed(request);
            long timeTakenMs = System.currentTimeMillis() - requestStarted;
            if (response.isSuccessful()) {
                log.info(format("OUT status=%s method=%s time=%dms url=%s", response.code(), request.method(), timeTakenMs, url));
            } else {
                log.warn(format("Request failed: status=%s method=%s time=%dms url=%s", response.code(), request.method(), timeTakenMs, url));
            }
            return response;
        } catch (Throwable exception) {
            long timeTakenMs = System.currentTimeMillis() - requestStarted;

            log.error(format("Request failed: method=%s time=%dms url=%s", request.method(), timeTakenMs, url), exception);
            throw exception;
        }
    }

    private static String toStringWithoutQuery(HttpUrl url) {
        String urlStr = url.toString();
        int queryParamStart = urlStr.indexOf("?");

        if (queryParamStart < 0) {
            return urlStr;
        }

        return urlStr.substring(0, queryParamStart);
    }

}
