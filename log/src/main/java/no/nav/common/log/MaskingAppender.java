package no.nav.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

public class MaskingAppender extends AppenderBase<ILoggingEvent> {

    private Appender<ILoggingEvent> appender;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        appender.doAppend(new MaskedLoggingEvent(iLoggingEvent));
    }

    @SuppressWarnings("unused")
    public void setAppender(Appender<ILoggingEvent> appender) {
        this.appender = appender;
    }

}
