package no.nav.common.auth.context;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Slf4j
public class AuthContextHolderThreadLocal implements AuthContextHolder {

    private static AuthContextHolderThreadLocal instance;

    private static final ThreadLocal<AuthContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private AuthContextHolderThreadLocal() {}

    public static AuthContextHolder instance() {
        if (instance == null) {
            instance = new AuthContextHolderThreadLocal();
        }

        return instance;
    }

    @Override
    public void withContext(AuthContext authContext, UnsafeRunnable runnable) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            runnable.run();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    @Override
    public <T> T withContext(AuthContext authContext, UnsafeSupplier<T> supplier) {
        AuthContext previousContext = CONTEXT_HOLDER.get();
        try {
            CONTEXT_HOLDER.set(authContext);
            return supplier.get();
        } finally {
            CONTEXT_HOLDER.set(previousContext);
        }
    }

    @Override
    public Optional<AuthContext> getContext() {
        return ofNullable(CONTEXT_HOLDER.get());
    }

    @Override
    public void setContext(AuthContext authContext) {
        CONTEXT_HOLDER.set(authContext);
    }

}
