package no.nav.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class CxfNonErrorStdoutFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        boolean isCxfLogger = loggerName.equals("org.apache.cxf")
                || loggerName.startsWith("org.apache.cxf.")
                || loggerName.equals("no.nav.common.cxf")
                || loggerName.startsWith("no.nav.common.cxf.");

        if (isCxfLogger && !event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }
}
