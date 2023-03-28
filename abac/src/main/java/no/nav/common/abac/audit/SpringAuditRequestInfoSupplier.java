package no.nav.common.abac.audit;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import static no.nav.common.rest.filter.LogRequestFilter.resolveCallId;
import static no.nav.common.rest.filter.LogRequestFilter.resolveConsumerId;


public class SpringAuditRequestInfoSupplier implements AuditRequestInfoSupplier {

    @Override
    public AuditRequestInfo get() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(requestAttributes -> requestAttributes instanceof ServletRequestAttributes)
                .map(requestAttributes -> (ServletRequestAttributes) requestAttributes)
                .map(ServletRequestAttributes::getRequest)
                .map(SpringAuditRequestInfoSupplier::utledRequestInfo)
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
