package no.nav.common.abac;

import no.nav.common.abac.audit.*;

public class VeilarbPepFactory {

    public static VeilarbPep get(String abacUrl,
                                 String srvUsername,
                                 String srvPassword,
                                 AuditConfig auditConfig) {
        AbacCachedClient abacClient = new AbacCachedClient(new AbacHttpClient(abacUrl, srvUsername, srvPassword));
        return new VeilarbPep(srvUsername, abacClient, new NimbusSubjectProvider(), auditConfig);
    }

    public static VeilarbPep get(String abacUrl,
                                 String srvUsername,
                                 String srvPassword,
                                 AuditRequestInfoSupplier auditRequestInfoSupplier,
                                 AuditLogFilter auditLogFilter) {
        AuditConfig auditConfig = new AuditConfig(new AuditLogger(), auditRequestInfoSupplier, auditLogFilter);

        return get(abacUrl, srvUsername, srvPassword, auditConfig);
    }

    public static VeilarbPep get(String abacUrl,
                                 String srvUsername,
                                 String srvPassword,
                                 AuditRequestInfoSupplier auditRequestInfoSupplier) {
        AuditConfig auditConfig = new AuditConfig(new AuditLogger(), auditRequestInfoSupplier, null);

        return get(abacUrl, srvUsername, srvPassword, auditConfig);
    }

    public static VeilarbPep get(String abacUrl,
                                 String srvUsername,
                                 String srvPassword) {
        return get(abacUrl, srvUsername, srvPassword, new AuditConfig(null, null, null));
    }
}
