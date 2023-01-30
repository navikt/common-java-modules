package no.nav.common.abac.audit;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static no.nav.common.rest.filter.JavaxLogRequestFilter.resolveCallId;
import static no.nav.common.rest.filter.JavaxLogRequestFilter.resolveConsumerId;

// TODO: Når vi har oppgradert til Spring Boot 3 og bumpet spring-web i common så kan vi fjerne Javax prefix og ta i bruk jakarta
@Deprecated
public class JavaxSpringAuditRequestInfoSupplier implements AuditRequestInfoSupplier {

    @Override
    public AuditRequestInfo get() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(requestAttributes -> requestAttributes instanceof ServletRequestAttributes)
                .map(requestAttributes -> (ServletRequestAttributes) requestAttributes)
                .map(ServletRequestAttributes::getRequest)
                .map(JavaxSpringAuditRequestInfoSupplier::utledRequestInfo)
                .orElse(null);
    }

    private static AuditRequestInfo utledRequestInfo(HttpServletRequest request) {
        return AuditRequestInfo.builder()
                .callId(resolveCallId(request))
                .consumerId(resolveConsumerId(request))
                .requestMethod(request.getMethod())
                .requestPath(request.getRequestURI())
                .build();
    }
}
