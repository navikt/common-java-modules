package no.nav.common.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Slf4j
public class LoginFilter implements Filter {

    private final List<LoginProvider> loginProviders;
    private final List<String> publicPaths;

    private List<Pattern> publicPatterns;

    public LoginFilter(List<LoginProvider> loginProviders, List<String> publicPaths) {
        this.loginProviders = loginProviders;
        this.publicPaths = publicPaths;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String contextPath = contextPath(filterConfig);
        this.publicPatterns = publicPaths.stream()
                .map(path -> "^" + contextPath + path)
                .map(Pattern::compile)
                .collect(toList());
        log.info("initialized {} with public patterns: {}", LoginFilter.class.getName(), publicPatterns);
    }

    private String contextPath(FilterConfig filterConfig) {
        String contextPath = filterConfig.getServletContext().getContextPath();
        if (contextPath == null || contextPath.length() <= 1) {
            contextPath = "";
        }
        return contextPath;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        Optional<Subject> optionalSubject = resolveSubject(httpServletRequest, httpServletResponse);
        if (optionalSubject.isPresent()) {
            SubjectHandler.withSubject(optionalSubject.get(), () -> chain.doFilter(request, response));
        } else if (isPublic(httpServletRequest)) {
            chain.doFilter(request, response);
        } else {
            unAuthenticated(httpServletRequest, httpServletResponse);
        }
    }

    private void unAuthenticated(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        Optional<String> optionalRedirectUrl = loginProviders.stream().flatMap(p -> p.redirectUrl(httpServletRequest, httpServletResponse).map(Stream::of).orElseGet(Stream::empty)).findFirst();
        if (acceptsHtml(httpServletRequest) && optionalRedirectUrl.isPresent()) {
            httpServletResponse.sendRedirect(optionalRedirectUrl.get());
        } else {
            httpServletResponse.setStatus(SC_UNAUTHORIZED);
        }
    }

    static boolean acceptsHtml(HttpServletRequest httpServletRequest) {
        return StringUtils.of(httpServletRequest.getHeader("Accept")).map(s->s.contains("*/*") || s.contains("text/html")).orElse(false);
    }

    boolean isPublic(HttpServletRequest httpServletRequest) {
        return publicPatterns.stream().anyMatch(p -> p.matcher(httpServletRequest.getRequestURI()).matches());
    }

    private Optional<Subject> resolveSubject(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return loginProviders.stream()
                .flatMap(loginProvider -> loginProvider.authenticate(httpServletRequest, httpServletResponse)
                        .map(Stream::of)
                        .orElseGet(Stream::empty)
                )
                .findFirst();
    }

    @Override
    public void destroy() {

    }
}
