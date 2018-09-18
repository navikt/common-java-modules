package no.nav.log;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

public class MaskedThrowableProxy implements IThrowableProxy {

    private final IThrowableProxy throwableProxy;

    private MaskedThrowableProxy(IThrowableProxy throwableProxy) {
        this.throwableProxy = throwableProxy;
    }

    @Override
    public String getMessage() {
        return MaskedLoggingEvent.mask(throwableProxy.getMessage());
    }

    @Override
    public String getClassName() {
        return throwableProxy.getClassName();
    }

    @Override
    public StackTraceElementProxy[] getStackTraceElementProxyArray() {
        return throwableProxy.getStackTraceElementProxyArray();
    }

    @Override
    public int getCommonFrames() {
        return throwableProxy.getCommonFrames();
    }

    @Override
    public IThrowableProxy getCause() {
        return MaskedThrowableProxy.mask(throwableProxy.getCause());
    }

    public static IThrowableProxy mask(IThrowableProxy throwableProxy) {
        return throwableProxy == null ? throwableProxy : new MaskedThrowableProxy(throwableProxy);
    }

    @Override
    public IThrowableProxy[] getSuppressed() {
        IThrowableProxy[] suppressed = throwableProxy.getSuppressed();
        IThrowableProxy[] maskedSuppressed = new IThrowableProxy[suppressed.length];
        for (int i = 0; i < suppressed.length; i++) {
            maskedSuppressed[i] = mask(suppressed[i]);
        }
        return maskedSuppressed;
    }
}
