package no.nav.apiapp.rest;

import no.nav.sbl.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public class NavCorsFilter implements Filter {
    public static final String ORIGIN = "Origin";
    public static final String CORS_ALLOWED_ORIGINS = "cors.allowed.origins";
    public static final String CORS_ALLOWED_METHODS = "cors.allowed.methods";
    public static final String CORS_ALLOWED_HEADERS = "cors.allowed.headers";
    public static final String CORS_ALLOWED_CREDENTIALS = "cors.allow.credentials";
    public static final String CORS_MAX_AGE = "cors.maxage";

    static final List<String> DEFAULT_ALLOWED_METHODS = asList("GET", "HEAD", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
    static final List<String> DEFAULT_ALLOWED_HEADERS = asList("Accept", "Accept-language", "Content-Language", "Content-Type");

    private static final CorsHeader CORS_ORIGIN = new OriginCorsHeader(
            "Access-Control-Allow-Origin",
            CORS_ALLOWED_ORIGINS,
            Collections.emptyList(),
            NavCorsFilter::validerAllowOrigin
    );
    private static final CorsHeader CORS_METHODS = new CorsHeader(
            "Access-Control-Allow-Methods",
            CORS_ALLOWED_METHODS,
            DEFAULT_ALLOWED_METHODS,
            NavCorsFilter::validerAllowMethod
    );
    private static final CorsHeader CORS_CREDENTIALS = new CorsHeader(
            "Access-Control-Allow-Credentials",
            CORS_ALLOWED_CREDENTIALS,
            "true",
            NavCorsFilter::validerAllowCredentials
    );

    private static final CorsHeader CORS_MAXAGE= new CorsHeader(
            "Access-Control-Max-Age",
            CORS_MAX_AGE,
            "3600",
            NavCorsFilter::validerMaxAge
    );

    private static final CorsHeader CORS_HEADERS= new CorsHeader(
            "Access-Control-Allow-Headers",
            CORS_ALLOWED_HEADERS,
            DEFAULT_ALLOWED_HEADERS,
            NavCorsFilter::validerAllowHeader
    );

    private static final List<CorsHeader> CORS_HEADER_LIST = asList(
            CORS_ORIGIN,
            CORS_METHODS,
            CORS_CREDENTIALS,
            CORS_MAXAGE,
            CORS_HEADERS
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String originHeader = httpServletRequest.getHeader(ORIGIN);

        if (validOrigin(originHeader, CORS_ORIGIN.value)) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            CORS_HEADER_LIST.forEach((corsHeader) -> corsHeader.apply(httpServletRequest, httpServletResponse));
            httpServletResponse.setHeader("Vary", "Origin");

            if (httpServletRequest.getMethod().equals("OPTIONS")) {
                httpServletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    static boolean validOrigin(String originHeader, List<String> allowedSubDomains) {
        return StringUtils.notNullOrEmpty(originHeader) &&
                allowedSubDomains.stream().filter(StringUtils::notNullOrEmpty).anyMatch(originHeader::endsWith);
    }

    @Override
    public void destroy() {

    }

    static String validerAllowOrigin(String subdomene) {
        if (subdomene.charAt(0) != '.') {
            throw new IllegalArgumentException("tillatt skal være subdomene, f.eks. '.nav.no' ikke 'nav.no'");
        }
        return subdomene;
    }

    private static String validerAllowMethod(String method) {
        if (!DEFAULT_ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("tillatt method skal være del av listen, " + String.join(", ", DEFAULT_ALLOWED_METHODS));
        }
        return method;
    }

    private static String validerAllowCredentials(String bool) {
        if (!("true".equals(bool) || "false".equals(bool))) {
            throw new IllegalArgumentException("credentials skal være 'true' eller 'false'");
        }
        return bool;
    }

    private static String validerMaxAge(String maxAge) {
        try {
            Integer.parseInt(maxAge, 10);
            return maxAge;
        } catch (Exception e) {
            throw new IllegalArgumentException("maxAge skal være ett tall", e);
        }
    }

    private static String validerAllowHeader(String header) {
        // Godtar det meste her
        return header;
    }

    public static class CorsHeader {
        final String header;
        final String environmentPropery;
        final List<String> value;
        final String stringValue;
        final List<String> defaultValue;
        final Function<String, String> validator;

        public CorsHeader(String header, String environmentPropery, String defaultValue, Function<String, String> validator) {
            this(header, environmentPropery, asList(defaultValue), validator);
        }

        public CorsHeader(String header, String environmentPropery, List<String> defaultValue, Function<String, String> validator) {
            this.header = header;
            this.environmentPropery = environmentPropery;
            this.defaultValue = defaultValue;
            this.validator = validator;
            this.value = getOptionalProperty(environmentPropery)
                    .map(string -> string.split(","))
                    .map(Stream::of)
                    .orElseGet(defaultValue::stream)
                    .map(String::trim)
                    .map(validator)
                    .collect(Collectors.toList());
            this.stringValue = String.join(", ", this.value);
        }

        void apply(HttpServletRequest request, HttpServletResponse response) {
            response.setHeader(header, stringValue);
        }
    }
    public static class OriginCorsHeader extends CorsHeader {
        public OriginCorsHeader(String header, String environmentPropery, List<String> defaultValue, Function<String, String> validator) {
            super(header, environmentPropery, defaultValue, validator);
        }

        @Override
        void apply(HttpServletRequest request, HttpServletResponse response) {
            response.setHeader(header, request.getHeader(ORIGIN));
        }
    }
}

