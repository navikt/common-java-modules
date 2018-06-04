package no.nav.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.encoder.LogstashEncoder;

public class NavEncoder extends LogstashEncoder {

    @Override
    public byte[] encode(ILoggingEvent iLoggingEvent) {
        return super.encode(new MaskedLoggingEvent(iLoggingEvent));
    }

}
