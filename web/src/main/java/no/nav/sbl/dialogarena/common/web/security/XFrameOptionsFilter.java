package no.nav.sbl.dialogarena.common.web.security;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Legger til X-Frame-Options på responsen for å unngå click-jacking angrep
 * </p>
 * <p>
 * Setter DENY per default - noe som betyr at filtrerte ressurser ikke kan frames
 * </p>
 * http://tools.ietf.org/html/draft-ietf-websec-x-frame-options-01
 */
public class XFrameOptionsFilter implements Filter {

    public static final String OPTION_INIT_PARAMETER_NAME = "option";

    static final String DENY_OPTION = "DENY";
    static final String SAMEORIGIN_OPTION = "SAMEORIGIN";

    private Set<String> availableOptions = new HashSet<>();

    {
        availableOptions.add(DENY_OPTION);
        availableOptions.add(SAMEORIGIN_OPTION);
    }

    static final String DEFAULT_OPTION = DENY_OPTION;

    static final String X_FRAME_OPTIONS_HEADER_NAME = "X-Frame-Options";

    private String option = DEFAULT_OPTION;

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {
        String initOption = filterConfig.getInitParameter(OPTION_INIT_PARAMETER_NAME);
        if (initOption != null && !initOption.isEmpty()) {

            if (availableOptions.contains(initOption)) {
                this.option = initOption;
            } else {
                throw new IllegalArgumentException("Init parameter option with value " + initOption + " is illegal.");
            }
        }
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader(X_FRAME_OPTIONS_HEADER_NAME, this.option);
        chain.doFilter(request, httpServletResponse);
    }

    @Override
    public void destroy() {

    }
}