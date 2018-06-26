package no.nav.common.auth.openam.sbs;

import no.nav.common.auth.LoginProvider;
import no.nav.common.auth.Subject;
import no.nav.sbl.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;


public class OpenAMLoginFilter implements LoginProvider {

    public static final String NAV_ESSO_COOKIE_NAVN = "nav-esso";

    private final OpenAMUserInfoService userInfoService;

    public OpenAMLoginFilter(OpenAmConfig openAmConfig) {
        this(new OpenAMUserInfoService(openAmConfig));
    }

    OpenAMLoginFilter(OpenAMUserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @Override
    public Optional<Subject> authenticate(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String requestEksternSsoToken = getRequestEksternSsoToken(httpServletRequest);
        if (StringUtils.nullOrEmpty(requestEksternSsoToken)) {
            return Optional.empty();
        } else {
            Optional<Subject> userInfo = userInfoService.convertTokenToSubject(requestEksternSsoToken);
            if (!userInfo.isPresent()) {
                removeSsoToken(httpServletRequest, httpServletResponse);
            }
            return userInfo;
        }
    }

    @Override
    public Optional<String> redirectUrl(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return Optional.empty();
    }

    private String getRequestEksternSsoToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(NAV_ESSO_COOKIE_NAVN)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void removeSsoToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(NAV_ESSO_COOKIE_NAVN)) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setDomain(".nav.no");
                    response.addCookie(cookie);
                }
            }
        }
    }

}
