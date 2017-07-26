package no.nav.brukerdialog.security.pingable;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.types.Pingable;

@SuppressWarnings("unused")
public class IssoSystemBrukerTokenHelsesjekk implements Pingable {

    private final SystemUserTokenProvider systemUserTokenProvider;

    public IssoSystemBrukerTokenHelsesjekk(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    public IssoSystemBrukerTokenHelsesjekk() {
        this(new SystemUserTokenProvider());
    }

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata(
                "ISSO via " + systemUserTokenProvider.openAmHost,
                "Sjekker applikasjonen har gyldig ISSO-jwt-token for systembruker",
                true
        );
        try {
            String token = systemUserTokenProvider.getToken();
            if (token != null && token.trim().length() > 0) {
                return Ping.lyktes(metadata);
            } else {
                return Ping.feilet(metadata, "mangler gyldig ISSO-jwt-token");
            }
        } catch (Throwable e) {
            return Ping.feilet(metadata, e);
        }
    }

}
