package no.nav.brukerdialog.security.pingable;

import no.nav.sbl.dialogarena.types.Pingable;

import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("unused")
public class IssoIsAliveHelsesjekk implements Pingable {

    @Override
    public Ping ping() {
        Ping.PingMetadata metadata = new Ping.PingMetadata(
                "ISSO via " + System.getProperty("isso.isalive.url", "isso.isalive.url"),
                "Sjekker om is-alive til ISSO svarer",
                true
        );
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
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
