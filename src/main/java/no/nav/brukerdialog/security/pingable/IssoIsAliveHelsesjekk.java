package no.nav.brukerdialog.security.pingable;

import no.nav.sbl.dialogarena.types.Pingable;

import java.net.HttpURLConnection;
import java.net.URL;

import static no.nav.brukerdialog.security.Constants.ISSO_ISALIVE_URL;

@SuppressWarnings("unused")
public class IssoIsAliveHelsesjekk implements Pingable {

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata(
                "ISSO via " + System.getProperty(ISSO_ISALIVE_URL, ISSO_ISALIVE_URL),
                "Sjekker om is-alive til ISSO svarer",
                true
        );
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty(ISSO_ISALIVE_URL)).openConnection();
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return Ping.lyktes(metadata);
            } else {
                return Ping.feilet(metadata, "Isalive returnerte statuskode: " + responseCode);
            }
        } catch (Throwable e) {
            return Ping.feilet(metadata, e);
        }
    }

}
