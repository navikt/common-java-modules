package no.nav.common.abac.audit;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class SpringAuditRequestInfoSupplier implements AuditRequestInfoSupplier {

    @Override
    public AuditRequestInfo get() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(requestAttributes -> requestAttributes instanceof ServletRequestAttributes)
                .map(requestAttributes -> (ServletRequestAttributes) requestAttributes)
                .map(ServletRequestAttributes::getRequest)
                .map(AuditRequestInfo::fraHttpServletRequest)
                .orElse(null);
    }
}
