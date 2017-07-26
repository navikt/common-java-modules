package no.nav.brukerdialog.security.pingable;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.types.Pingable;

import static no.nav.brukerdialog.security.Constants.ISSO_HOST_URL_PROPERTY_NAME;
import static no.nav.brukerdialog.tools.Utils.getSystemProperty;

@SuppressWarnings("unused")
public class IssoSystemBrukerTokenHelsesjekk implements Pingable {

    private final String issoHostUrl;

    public IssoSystemBrukerTokenHelsesjekk(String issoHostUrl) {
        this.issoHostUrl = issoHostUrl;
    }

    public IssoSystemBrukerTokenHelsesjekk() {
        this(getSystemProperty(ISSO_HOST_URL_PROPERTY_NAME));
    }

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata(
                "ISSO via " + issoHostUrl,
                "Sjekker applikasjonen har gyldig ISSO-jwt-token for systembruker",
                true
        );
        try {
            String token = new SystemUserTokenProvider().getToken();
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
