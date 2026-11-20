package no.nav.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluatorBase;

/**
 * Matcher CXF-meldinger som EvaluatorFilter skal avvise fra stdout.
 * CXF kan logge SOAP-innhold; meldinger utenom ERROR skal derfor ikke til vanlig logg.
 * ERROR slipper gjennom til MaskingAppender. Eventuell logging til secureLog styres separat.
 * Bruker samme delstrengsjekk som det tidligere Janino-uttrykket, ikke en prefikssjekk.
 */
public class CxfNonErrorEventEvaluator extends EventEvaluatorBase<ILoggingEvent> {

    @Override
    public boolean evaluate(ILoggingEvent event) {
        return event.getLevel() != Level.ERROR
                && (event.getLoggerName().contains("org.apache.cxf")
                        || event.getLoggerName().contains("no.nav.common.cxf"));
    }
}
