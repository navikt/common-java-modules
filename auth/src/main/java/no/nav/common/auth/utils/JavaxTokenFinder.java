package no.nav.common.auth.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Deprecated
public interface JavaxTokenFinder {

    Optional<String> findToken(HttpServletRequest request);

}
