package no.nav.common.auth.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface TokenFinder {

    Optional<String> findToken(HttpServletRequest request);

}
