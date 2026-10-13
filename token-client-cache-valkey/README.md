# Token Client Cache Valkey

Optional extension module for sharing token-client cache entries between pods of the same application.
Keeping Lettuce in this module means applications using only `token-client` retain their existing
dependencies and default Caffeine cache.

## Dependency

```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>token-client-cache-valkey</artifactId>
    <version>NEWEST_VERSION</version>
</dependency>
```

This module depends on `token-client`, so applications may use this dependency instead of declaring
both explicitly.

## Usage

Configuration is injected by the application. The module does not read Nais environment variables.

```java
RedisURI redisUri = RedisURI.Builder.redis(valkeyHost, valkeyPort)
        .withAuthentication(valkeyUsername, valkeyPassword)
        .withSsl(true)
        .withVerifyPeer(SslVerifyMode.FULL)
        .build();

ManagedValkeyTokenCache valkeyCache = ManagedValkeyTokenCache.connect(
        redisUri,
        "<cluster>:<namespace>:<application>:<azure-client-id>",
        Duration.ofMillis(100)
);

AzureAdOnBehalfOfTokenClient tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .withCache(valkeyCache)
        .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
        .buildOnBehalfOfTokenClient();
```

Keep `ManagedValkeyTokenCache` for the application lifetime and call `close()` during shutdown. It
owns and closes its Lettuce connection and client. Applications that already manage a connection
may instead construct `ValkeyTokenCache` with `connection.sync()`; the application then owns the
connection and client lifecycle and must configure bounded command timeouts itself.
`ManagedValkeyTokenCache.connect(...)` is fail-fast during initial connection setup. Applications
that require startup without Valkey must handle that setup failure explicitly and choose whether to
start without a cache.

The namespace must be identical across pods of one application and distinct between environments,
applications and Azure clients. Both namespace and cache identity are hashed before storage. The
assertion key strategy additionally isolates client ID, token endpoint, scope and incoming token.
Renewed incoming tokens therefore use a new cache entry.

## Expiration

The default safety margin is 30 seconds. Cache entries expire at the outgoing JWT's `exp` minus this
margin, using atomic `SET ... PXAT`. Tokens already inside the margin are returned but not cached.
Expired tokens and tokens without a valid expiration are never cached.

## Failure behavior

- A Valkey read failure is logged and treated as a cache miss. Token exchange is attempted.
- A Valkey write failure is logged but does not fail an otherwise successful token exchange.
- Warnings are sanitized and limited to one per minute for reads and writes separately.
- Provider and token exchange failures are not swallowed.
- The managed client adds no retries and prevents Lettuce from replaying queued commands after a
  reconnect. It rejects commands while disconnected, limits the request queue to 100 and applies the
  configured timeout to connection establishment and commands.
- Lettuce reconnects in the background so later requests can resume using the cache.

Valkey unavailability can therefore cause one token exchange per request. The cache intentionally
does not include a local fallback, distributed lock or single-flight mechanism. Concurrent misses
may perform multiple exchanges.

## Design constraints

- Use the existing `TokenCache` interface and `withCache(...)` builder option.
- Require an explicit namespace shared by an application's pods, but distinct between applications
  and environments.
- Use assertion-based Azure AD OBO cache keys to isolate clients, token endpoints, scopes and
  incoming tokens. Do not expose incoming tokens or user identifiers in stored keys.
- Derive TTL from the outgoing token's actual expiration, minus a safety margin.
- Inject configuration explicitly; either the managed cache or the application owns client shutdown.
- Treat read failures as misses and preserve successful token exchanges after write failures,
  with explicit sanitized reporting and bounded waiting.
- Do not add a local fallback cache or distributed locking.

Tokens are sensitive bearer credentials. Production usage requires TLS with certificate verification,
restricted network access and credentials, and an explicit decision about persistence and backups.
Limit Valkey access to the owning application, and do not share namespaces or instances across apps
without equivalent ACL isolation. Persistence and backups may retain tokens until their own retention
expires even after cache TTL deletion. Never log tokens, credentials, cache keys or namespaces.
No application data beyond OBO tokens is stored by this module.
