package no.nav.common.feed.common;

import javax.ws.rs.client.Invocation;

public interface OutInterceptor {
    void apply(Invocation.Builder builder);
}
