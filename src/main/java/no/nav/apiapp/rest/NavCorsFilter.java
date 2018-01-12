package no.nav.apiapp.rest;

import no.nav.sbl.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public class NavCorsFilter implements Filter {

    public static final String ORIGIN = "Origin";
    public static final String CORS_ALLOWED_ORIGINS = "cors.allowed.origins";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final List<String> ALLOWED_ORIGINS = getAllowedOrigins();
    public static final String ALLOWED_METHODS = Arrays.asList(
            "GET",
            "HEAD",
            "POST",
            "PATCH",
            "PUT",
            "DELETE",
            "OPTIONS"
    ).stream().collect(joining(", "));

    static List<String> getAllowedOrigins() {
        return getOptionalProperty(CORS_ALLOWED_ORIGINS)
                .map(string -> string.split(","))
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String originHeader = httpServletRequest.getHeader(ORIGIN);

        if (validOrigin(originHeader)) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);
            httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpServletResponse.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        }
        chain.doFilter(request, response);
    }

    private boolean validOrigin(String originHeader) {
        return validOrigin(originHeader, ALLOWED_ORIGINS);
    }

    static boolean validOrigin(String originHeader, List<String> allowedOrigins) {
        return StringUtils.notNullOrEmpty(originHeader) &&
                allowedOrigins.stream().filter(StringUtils::notNullOrEmpty).anyMatch(originHeader::endsWith);
    }

    @Override
    public void destroy() {

    }
}
