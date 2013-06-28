package no.nav.sbl.dialogarena.common.web.security;

import no.nav.modig.security.tilgangskontroll.policy.pep.EnforcementPoint;
import no.nav.modig.security.tilgangskontroll.wicket.BehaviorPolicyAuthorizationStrategy;
import org.apache.wicket.core.util.crypt.KeyInSessionSunJceCryptFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.util.crypt.ICryptFactory;


public final class ApplicationSecurityConfig {

    private final EnforcementPoint pep;

    private ICryptFactory cryptFactory = new KeyInSessionSunJceCryptFactory();

    private boolean enforceMounts = true;


    public ApplicationSecurityConfig(EnforcementPoint pep) {
        this.pep = pep;
    }

    public ApplicationSecurityConfig withCryptFactory(ICryptFactory cryptFactory) {
        this.cryptFactory = cryptFactory;
        return this;
    }

    public ApplicationSecurityConfig doNotEnforcedMounts() {
        this.enforceMounts = false;
        return this;
    }


    public void configure(WebApplication application) {
        ISecuritySettings securitySettings = application.getSecuritySettings();
        securitySettings.setEnforceMounts(enforceMounts);
        securitySettings.setCryptFactory(cryptFactory);
        application.setRootRequestMapper(new ModigCryptoMapper(application.getRootRequestMapper(), cryptFactory));
        securitySettings.setAuthorizationStrategy(new BehaviorPolicyAuthorizationStrategy(pep));
    }
}
