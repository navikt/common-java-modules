package no.nav.apiapp.security;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.FeilMapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

@Slf4j
public class InternalProtectionFilter implements Filter {

    private static final Pattern SUBNET_IP_PATTERN = Pattern.compile("192\\.168\\.\\d{1,3}\\.\\d{1,3}");
    private final List<String> whitelistedDomains;

    public InternalProtectionFilter(List<String> whitelistedDomains) {
        this.whitelistedDomains = new ArrayList<>(whitelistedDomains);
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String hostName = httpServletRequest.getServerName();
        if (isAllowedAccessToInternal(hostName)) {
            chain.doFilter(request, response);
        } else {
            log.warn("blocking access to unsafe host: {}", hostName);
            httpServletResponse.setStatus(SC_FORBIDDEN);
            httpServletResponse.setContentType("text/plain");
            PrintWriter printWriter = httpServletResponse.getWriter();
            printWriter.write("FORBIDDEN");

            if (FeilMapper.visDetaljer()) {
                printWriter.write(String.format("\n\n%s must be whitelisted in %s", hostName, getClass().getName()));
            }
        }
    }

    boolean isAllowedAccessToInternal(String serverName) {
        return "localhost".equals(serverName) // allow for local development and testing
                || SUBNET_IP_PATTERN.matcher(serverName).matches() // kubernetes uses subnet ips to access internal endpoints
                || whitelistedDomains.stream().anyMatch(serverName::endsWith) // allow access from internal addresses
                ;
    }

    @Override
    public void destroy() {
    }

}
