package no.nav.apiapp;

import no.nav.sbl.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.Q;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.T;
import static no.nav.sbl.util.EnvironmentUtils.isEnvironmentClass;
import static no.nav.sbl.util.StringUtils.notNullOrEmpty;

public class ComplianceEnforcementFilter implements Filter {

    private static final Set<String> WHITELIST = new HashSet<>(asList(
            "mozilla", // seems to include all major browsers - http://www.networkinghowtos.com/howto/common-user-agent-list/
            "curl"
    ));
    private static final Set<Function<HttpServletRequest, Stream<String>>> RULES = new HashSet<>(asList(
            request -> notNullOrEmpty(request.getHeader(CONSUMER_ID_HEADER_NAME)) ? empty() : of(
                    "provide consumer id (typically application name) in the header: " + CONSUMER_ID_HEADER_NAME
            ),
            request -> notNullOrEmpty(request.getHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME)) ? empty() : of(
                    "provide call id (a correlation id) in the header: " + PREFERRED_NAV_CALL_ID_HEADER_NAME
            )
    ));

    private final boolean complianceEnforcementIsActive = isEnvironmentClass(Q) || isEnvironmentClass(T);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (enforceCompliance(request)) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            List<String> errorMessages = RULES.stream().flatMap(r -> r.apply(httpServletRequest)).collect(toList());
            if (!errorMessages.isEmpty()) {
                httpServletResponse.setStatus(400);
                httpServletResponse.setContentType(TEXT_PLAIN);
                httpServletResponse.getWriter().write(String.format("" +
                                "This looks like a request from an application that does not conform to the following compliance rules at NAV:%s" +
                                "Please update your application to follow these rules." +
                                "\n\nYou will not receive this error in production." +
                                "",
                        errorMessages.stream().map(s -> " - " + s).collect(joining("\n", "\n\n", "\n\n"))
                ));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean enforceCompliance(ServletRequest request) {
        return complianceEnforcementIsActive // never activate in production. allow for performance optimizations by using a final field
                && isApplicationRequest(request) // target applications only, not frontend(applications), testers, developers
                && !isInternalRequest(request); // k8s is allowed to access internal resources
    }

    private boolean isApplicationRequest(ServletRequest servletRequest) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userAgent = httpServletRequest.getHeader(USER_AGENT);
        return userAgent == null || WHITELIST.stream().noneMatch(charSequence -> userAgent.toLowerCase().contains(charSequence));
    }

    private boolean isInternalRequest(ServletRequest servletRequest) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        return httpServletRequest.getRequestURI().contains("/internal");
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }
}
