package no.nav.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;

public class MaskingAppender extends AsyncAppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        super.append(new MaskedLoggingEvent(iLoggingEvent));
    }

}
