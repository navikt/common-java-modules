package no.nav.common.feed.common;

@FunctionalInterface
public interface FeedAuthorizationModule {
    boolean isRequestAuthorized(String feedname);
}
